"""AI client OpenAI-compatible (spec §11).

Pagar: timeout 30s, retry 2x backoff, parse JSON tahan markdown,
fallback aman (score 0 / WAIT) saat parse gagal, key tidak pernah di-log.
"""
from __future__ import annotations

import asyncio
import json
import logging
import re
from typing import Any

import httpx

from ..config import Settings
from ..models.signal import AiVerdict, Signal, Verdict
from .prompt_builder import build_chat_messages, build_messages

log = logging.getLogger(__name__)

JSON_RE = re.compile(r"\{[\s\S]*\}")


class AiError(RuntimeError):
    def __init__(self, message: str, code: int | str | None = None) -> None:
        super().__init__(message)
        self.code = code


def parse_verdict(text: str, signal_id: str, model: str) -> AiVerdict:
    """Ambil JSON dari respons; fallback aman bila gagal (tidak pernah raise)."""
    candidate = text.strip()
    match = JSON_RE.search(candidate)
    if match:
        candidate = match.group(0)
    try:
        data = json.loads(candidate)
        verdict = Verdict(str(data.get("verdict", "WAIT")).upper())
        score = int(float(data.get("ai_score", 0)))
        risks = [str(r) for r in (data.get("risks") or [])][:5]
        return AiVerdict(
            signal_id=signal_id,
            ai_score=max(0, min(100, score)),
            verdict=verdict,
            reasoning=str(data.get("reasoning", ""))[:1000],
            risks=risks,
            model_used=model,
        )
    except Exception as exc:  # noqa: BLE001
        log.warning("parse AI gagal: %s", exc)
        return AiVerdict(
            signal_id=signal_id,
            ai_score=0,
            verdict=Verdict.WAIT,
            reasoning="AI parse error — verdict tidak dipakai",
            risks=["Respons AI tidak valid; andalkan skor kuantitatif."],
            model_used=model,
        )


class AiClient:
    def __init__(self, settings: Settings, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self._s = settings
        self._transport = transport

    def _sanitize(self, text: str) -> str:
        key = self._s.ai_api_key
        return text.replace(key, "***") if key else text

    async def _post(self, payload: dict[str, Any]) -> str:
        if not self._s.ai_api_key:
            raise AiError("AI_API_KEY belum diset", "no_key")

        url = f"{self._s.ai_base_url.rstrip('/')}/chat/completions"
        headers = {"Authorization": f"Bearer {self._s.ai_api_key}",
                   "Content-Type": "application/json"}
        last: Exception | None = None

        for attempt in range(self._s.ai_max_retries + 1):
            try:
                async with httpx.AsyncClient(timeout=self._s.ai_timeout_sec,
                                             transport=self._transport) as client:
                    r = await client.post(url, headers=headers, json=payload)
                if r.status_code == 429:
                    raise AiError("rate limit provider AI", 429)
                if r.status_code >= 500:
                    # 5xx = gangguan provider -> layak retry, jangan langsung menyerah
                    last = AiError(self._sanitize(f"provider error {r.status_code}"), r.status_code)
                    await asyncio.sleep(2 ** attempt)
                    continue
                if r.status_code >= 400:
                    raise AiError(self._sanitize(r.text[:300]), r.status_code)
                body = r.json()
                choices = body.get("choices") or []
                if not choices:
                    raise AiError("respons AI tanpa choices", "empty")
                return str(choices[0]["message"]["content"])
            except AiError:
                raise
            except Exception as exc:  # noqa: BLE001
                last = exc
                await asyncio.sleep(2 ** attempt)

        raise AiError(self._sanitize(f"gagal menghubungi AI: {last}"), "http")

    async def ping(self) -> dict[str, Any]:
        """Cek kredensial + endpoint tanpa biaya token (GET /models)."""
        if not self._s.ai_api_key:
            raise AiError("AI_API_KEY belum diset", "no_key")
        url = f"{self._s.ai_base_url.rstrip('/')}/models"
        headers = {"Authorization": f"Bearer {self._s.ai_api_key}"}
        try:
            async with httpx.AsyncClient(timeout=self._s.ai_timeout_sec,
                                         transport=self._transport) as client:
                r = await client.get(url, headers=headers)
        except Exception as exc:  # noqa: BLE001
            raise AiError(self._sanitize(f"endpoint AI tidak terjangkau: {exc}"), "http") from exc
        if r.status_code >= 400:
            raise AiError(self._sanitize(f"HTTP {r.status_code} dari provider AI"), r.status_code)
        try:
            models = [m.get("id") for m in r.json().get("data", []) if isinstance(m, dict)]
        except ValueError:
            models = []
        return {"ok": True, "models": models[:50], "model_configured": self._s.ai_model}

    async def analyze(self, signal: Signal, extra: dict | None = None) -> AiVerdict:
        text = await self._post({
            "model": self._s.ai_model,
            "messages": build_messages(signal, extra),
            "temperature": 0.2,
        })
        return parse_verdict(text, signal.id, self._s.ai_model)

    async def chat(self, signal: Signal, message: str,
                  history: list[dict] | None = None) -> str:
        return await self._post({
            "model": self._s.ai_model,
            "messages": build_chat_messages(signal, message, history),
            "temperature": 0.3,
        })

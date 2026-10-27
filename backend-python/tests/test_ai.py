"""Sprint 3: AI layer — parsing, retry, fallback, matriks keputusan."""
from __future__ import annotations

import asyncio
import json
from datetime import datetime, timezone

import httpx
import pytest
from fastapi.testclient import TestClient

from app.api import routes_ai
from app.config import Settings
from app.deps import get_store
from app.main import app
from app.models.signal import (
    AiVerdict,
    Confluence,
    CoreStrategy,
    Direction,
    Signal,
    SignalContext,
    Verdict,
)
from app.services.ai_client import AiClient, AiError, parse_verdict
from app.services.decision import final_decision
from app.services.prompt_builder import SYSTEM_PROMPT, build_messages

GOOD = json.dumps({
    "ai_score": 72, "verdict": "TAKE",
    "reasoning": "FVG searah tren H1, RSI netral.",
    "risks": ["NFP 2 jam lagi"],
})


def _signal(quant: int = 78) -> Signal:
    return Signal(
        pair="XAUUSD", timeframe="M15", direction=Direction.LONG,
        core_strategy=CoreStrategy.FVG, confirmations=[],
        entry=2650.5, stop_loss=2645.0, take_profit=[2661.5, 2667.0, 2672.5],
        risk_reward=2.0, quant_score=quant,
        confluence=Confluence(core_passed=True, rsi_passed=True, atr_passed=True,
                              volume_passed=False, ob_passed=True,
                              total_confluence=3, threshold=2),
        context=SignalContext(current_price=2650.5, atr=3.2, rsi=42.5,
                              trend="BULLISH", session="LONDON"),
        created_at=datetime(2026, 10, 27, 10, 0, tzinfo=timezone.utc),
    )


def _ai_client(handler, key: str = "ai-test-key") -> AiClient:
    cfg = Settings(ai_api_key=key, ai_base_url="https://ai.test/v1", ai_model="test-model",
                   ai_max_retries=2)
    return AiClient(cfg, transport=httpx.MockTransport(handler))


def _ok_handler(content: str):
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"choices": [{"message": {"content": content}}]})
    return handler


# ---------------- parsing ----------------
def test_parse_plain_json():
    v = parse_verdict(GOOD, "sig-1", "m")
    assert (v.ai_score, v.verdict) == (72, Verdict.TAKE)
    assert v.risks == ["NFP 2 jam lagi"]


def test_parse_json_inside_markdown_fence():
    text = f"Here is my verdict:\n```json\n{GOOD}\n```\nSemoga membantu."
    v = parse_verdict(text, "sig-1", "m")
    assert v.ai_score == 72 and v.verdict is Verdict.TAKE


def test_parse_garbage_falls_back_safely():
    v = parse_verdict("maaf saya tidak bisa menjawab", "sig-1", "m")
    assert v.ai_score == 0
    assert v.verdict is Verdict.WAIT
    assert "parse error" in v.reasoning.lower()


def test_parse_clamps_out_of_range_score():
    text = json.dumps({"ai_score": 250, "verdict": "take", "reasoning": "x", "risks": []})
    v = parse_verdict(text, "sig", "m")
    assert v.ai_score == 100
    assert v.verdict is Verdict.TAKE


# ---------------- client ----------------
def test_missing_key_raises_no_key():
    client = _ai_client(_ok_handler(GOOD), key="")
    with pytest.raises(AiError) as exc:
        asyncio.run(client.analyze(_signal()))
    assert exc.value.code == "no_key"


def test_analyze_returns_verdict_and_never_sends_levels_advice():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["body"] = json.loads(request.content)
        captured["auth"] = request.headers.get("authorization")
        return httpx.Response(200, json={"choices": [{"message": {"content": GOOD}}]})

    verdict = asyncio.run(_ai_client(handler).analyze(_signal()))
    assert verdict.ai_score == 72
    assert captured["auth"].startswith("Bearer ")
    system = captured["body"]["messages"][0]["content"]
    assert "position sizing" in system and "leverage" in system
    assert "Do NOT invent levels" in system


def test_retry_then_success():
    calls = {"n": 0}

    def handler(request: httpx.Request) -> httpx.Response:
        calls["n"] += 1
        if calls["n"] == 1:
            return httpx.Response(500, json={"error": "boom"})
        return httpx.Response(200, json={"choices": [{"message": {"content": GOOD}}]})

    verdict = asyncio.run(_ai_client(handler).analyze(_signal()))
    assert calls["n"] == 2
    assert verdict.verdict is Verdict.TAKE


def test_rate_limit_raises_429():
    client = _ai_client(lambda request: httpx.Response(429, json={"error": "slow down"}))
    with pytest.raises(AiError) as exc:
        asyncio.run(client.analyze(_signal()))
    assert exc.value.code == 429


def test_error_message_hides_key():
    secret = "sk-should-not-leak"
    client = _ai_client(lambda request: httpx.Response(401, text=f"invalid key {secret}"), key=secret)
    with pytest.raises(AiError) as exc:
        asyncio.run(client.analyze(_signal()))
    assert secret not in str(exc.value)


def test_prompt_includes_quant_context():
    messages = build_messages(_signal())
    payload = messages[1]["content"]
    assert "2650.5" in payload and "quant_score" in payload
    assert "TAKE" in SYSTEM_PROMPT


# ---------------- decision matrix ----------------
def test_decision_skip_when_quant_low():
    ai = AiVerdict(signal_id="s", ai_score=90, verdict=Verdict.TAKE, reasoning="", model_used="m")
    d = final_decision(40, ai)
    assert d.verdict is Verdict.SKIP


def test_decision_ai_cannot_upgrade_quant_skip():
    ai = AiVerdict(signal_id="s", ai_score=95, verdict=Verdict.TAKE, reasoning="", model_used="m")
    assert final_decision(49, ai).verdict is Verdict.SKIP


def test_decision_take_requires_both_above_60():
    ai = AiVerdict(signal_id="s", ai_score=72, verdict=Verdict.TAKE, reasoning="", model_used="m")
    assert final_decision(78, ai).verdict is Verdict.TAKE
    weak = AiVerdict(signal_id="s", ai_score=55, verdict=Verdict.TAKE, reasoning="", model_used="m")
    assert final_decision(78, weak).verdict is Verdict.WAIT


def test_decision_ai_skip_vetoes_high_quant():
    ai = AiVerdict(signal_id="s", ai_score=20, verdict=Verdict.SKIP, reasoning="", model_used="m")
    assert final_decision(90, ai).verdict is Verdict.SKIP


def test_decision_without_ai():
    assert final_decision(75, None).verdict is Verdict.TAKE
    assert final_decision(65, None).verdict is Verdict.WAIT


# ---------------- routes ----------------
def test_analyze_unknown_signal_404(monkeypatch):
    client = TestClient(app)
    r = client.post("/api/ai/analyze", json={"signal_id": "tidak-ada"})
    assert r.status_code == 404


def test_analyze_stores_verdict_with_final_decision(monkeypatch):
    store = get_store()
    sig = _signal()
    asyncio.run(store.add(sig))

    fake = _ai_client(_ok_handler(GOOD))
    monkeypatch.setattr(routes_ai, "ai_client_for", lambda *a, **k: fake)

    client = TestClient(app)
    r = client.post("/api/ai/analyze", json={"signal_id": sig.id})
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["ai_score"] == 72
    assert body["final_decision"] == "TAKE"

    again = client.get(f"/api/ai/verdict/{sig.id}")
    assert again.status_code == 200
    assert again.json()["final_decision"] == "TAKE"

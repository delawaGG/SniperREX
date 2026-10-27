"""Prompt untuk AI second-opinion (spec §11). AI tidak boleh naikkan status."""
from __future__ import annotations

import json

from ..models.signal import Signal

SYSTEM_PROMPT = """You are a professional forex/gold trading analyst reviewing a
deterministic quantitative signal. Give a second opinion only.

Rules you must obey:
- Do NOT recommend position sizing, lot size, leverage, or account risk in currency.
- Do NOT invent levels; use only entry, stop_loss, take_profit given.
- Be skeptical: if confluence is thin or the entry is chasing an exhausted move, say so.
- Answer in Bahasa Indonesia for "reasoning" and "risks".

Output ONLY valid JSON, no markdown, matching:
{
  "ai_score": <int 0-100>,
  "verdict": "TAKE" | "WAIT" | "SKIP",
  "reasoning": "<2-3 sentences>",
  "risks": ["<risk 1>", "<risk 2>"]
}

Scoring guide:
- 80-100: high probability, all confluences align
- 60-79: good setup, minor concerns
- 40-59: marginal, needs more confirmation
- 0-39: poor setup, avoid
"""


def build_signal_payload(signal: Signal, extra: dict | None = None) -> dict:
    payload = {
        "pair": signal.pair,
        "timeframe": signal.timeframe,
        "direction": signal.direction.value,
        "core_strategy": signal.core_strategy.value,
        "structure_event": signal.structure_event.value,
        "entry": signal.entry,
        "stop_loss": signal.stop_loss,
        "take_profit": signal.take_profit,
        "risk_reward_tp1": signal.risk_reward,
        "quant_score": signal.quant_score,
        "confluence": signal.confluence.model_dump(),
        "context": signal.context.model_dump(),
        "bar_closed_at": signal.created_at.isoformat(),
    }
    if extra:
        payload["checks"] = extra
    return payload


def build_messages(signal: Signal, extra: dict | None = None) -> list[dict[str, str]]:
    user = (
        "Review this signal and return the JSON verdict.\n\n"
        + json.dumps(build_signal_payload(signal, extra), ensure_ascii=False, indent=2)
    )
    return [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user},
    ]


def build_chat_messages(signal: Signal, message: str, history: list[dict] | None = None) -> list[dict[str, str]]:
    context = (
        "Kamu analis trading. Jawab singkat, maksimal 5 kalimat, Bahasa Indonesia. "
        "Jangan pernah menyarankan lot size, leverage, atau nominal risiko.\n\n"
        "Konteks signal:\n"
        + json.dumps(build_signal_payload(signal), ensure_ascii=False, indent=2)
    )
    msgs: list[dict[str, str]] = [{"role": "system", "content": context}]
    for item in history or []:
        if item.get("role") in ("user", "assistant") and item.get("content"):
            msgs.append({"role": item["role"], "content": str(item["content"])[:2000]})
    msgs.append({"role": "user", "content": message[:2000]})
    return msgs

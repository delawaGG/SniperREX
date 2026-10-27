"""Matriks keputusan final (v1.1 patch #8): AI tidak bisa menaikkan status."""
from __future__ import annotations

from dataclasses import dataclass, field

from ..config import Settings
from ..models.signal import AiVerdict, Verdict

QUANT_SKIP_BELOW = 50
TAKE_BOTH_MIN = 60
QUANT_TAKE_ALONE = 70


@dataclass
class Decision:
    verdict: Verdict
    reasons: list[str] = field(default_factory=list)


def final_decision(quant_score: int, ai: AiVerdict | None, cfg: Settings | None = None) -> Decision:
    reasons: list[str] = []

    if quant_score < QUANT_SKIP_BELOW:
        reasons.append(f"quant_score {quant_score} < {QUANT_SKIP_BELOW}")
        if ai is not None:
            reasons.append(f"AI: {ai.verdict.value} ({ai.ai_score})")
        return Decision(Verdict.SKIP, reasons)

    if ai is None:
        if quant_score >= QUANT_TAKE_ALONE:
            return Decision(Verdict.TAKE, [f"quant_score {quant_score} >= {QUANT_TAKE_ALONE}, tanpa AI"])
        return Decision(Verdict.WAIT, [f"quant_score {quant_score} belum cukup untuk TAKE tanpa AI"])

    reasons.append(f"AI verdict {ai.verdict.value} (score {ai.ai_score})")
    if ai.verdict is Verdict.SKIP:
        return Decision(Verdict.SKIP, reasons + ["AI menolak setup"])

    if quant_score >= TAKE_BOTH_MIN and ai.ai_score >= TAKE_BOTH_MIN:
        return Decision(Verdict.TAKE, reasons + [
            f"quant {quant_score} & AI {ai.ai_score} keduanya >= {TAKE_BOTH_MIN}"])

    return Decision(Verdict.WAIT, reasons + ["butuh konfirmasi tambahan sebelum entry"])

"""Quant score deterministik 0-100 (spec §10). BUKAN AI."""
from __future__ import annotations

from dataclasses import dataclass, asdict
from math import ceil

from ..config import Settings


@dataclass(frozen=True)
class ScoreBreakdown:
    core: int
    confluence: int
    rr: int
    trend: int
    volatility: int
    total: int

    def as_dict(self) -> dict[str, int]:
        return asdict(self)


def required_threshold(selected: int, cfg: Settings) -> int:
    """Gate konfluensi hybrid (v1.1).

    - 4 filter dipilih  -> FIXED 3 dari 4 (spec §8)
    - < 4 filter dipilih -> PROPORSIONAL ceil(ratio * n), minimal 2
    """
    if selected >= 4:
        return cfg.confluence_threshold_fixed
    return max(cfg.confluence_min_threshold, min(selected, ceil(cfg.confluence_ratio * selected)))


def _core_points(strength: float, cfg: Settings) -> int:
    strength = max(0.0, min(1.0, strength))
    return round(cfg.score_weight_core * strength)


def _confluence_points(passed: int, selected: int, cfg: Settings) -> int:
    if selected <= 0:
        return 0
    ratio = passed / selected
    if ratio >= 1.0:
        return cfg.score_weight_confluence
    if ratio >= 0.75:
        return round(cfg.score_weight_confluence * 0.72)   # 25 -> 18
    if ratio >= 0.5:
        return round(cfg.score_weight_confluence * 0.40)   # 25 -> 10
    return 0


def _rr_points(rr_extension: float, cfg: Settings) -> int:
    if rr_extension >= 3.0:
        return cfg.score_weight_rr
    if rr_extension >= 2.5:
        return round(cfg.score_weight_rr * 0.75)
    if rr_extension >= 2.0:
        return round(cfg.score_weight_rr * 0.5)
    return round(cfg.score_weight_rr * 0.25)


def _trend_points(alignment: str, cfg: Settings) -> int:
    return {"aligned": cfg.score_weight_trend,
            "mixed": round(cfg.score_weight_trend * 0.47),   # 15 -> 7
            "counter": 0}.get(alignment, 0)


def _volatility_points(atr_pct: float, cfg: Settings) -> int:
    if cfg.atr_pct_sweet_low <= atr_pct <= cfg.atr_pct_sweet_high:
        return cfg.score_weight_volatility
    near_low = cfg.atr_pct_sweet_low * 0.5
    near_high = cfg.atr_pct_sweet_high * 1.5
    if near_low <= atr_pct <= near_high:
        return round(cfg.score_weight_volatility * 0.5)
    return 0


def quant_score(
    *,
    core_strength: float,
    confirmations_passed: int,
    confirmations_selected: int,
    rr_extension: float,
    trend_alignment: str,
    atr_pct: float,
    cfg: Settings,
) -> ScoreBreakdown:
    parts = ScoreBreakdown(
        core=_core_points(core_strength, cfg),
        confluence=_confluence_points(confirmations_passed, confirmations_selected, cfg),
        rr=_rr_points(rr_extension, cfg),
        trend=_trend_points(trend_alignment, cfg),
        volatility=_volatility_points(atr_pct, cfg),
        total=0,
    )
    total = max(0, min(100, parts.core + parts.confluence + parts.rr + parts.trend + parts.volatility))
    return ScoreBreakdown(parts.core, parts.confluence, parts.rr, parts.trend, parts.volatility, total)

"""FVG (spec §7.4) — bullish & bearish. Invalid hanya bila zona tertutup PENUH."""
from __future__ import annotations

import pandas as pd

from ..config import Settings
from ..indicators import atr as atr_series
from ..models.signal import Direction, StructureEvent
from .base import SignalDraft

LOOKBACK = 60


def _gaps(df: pd.DataFrame) -> list[tuple[int, Direction, float, float]]:
    """(index candle-3, arah, batas_bawah, batas_atas).

    Bullish: low[i] > high[i-2]  -> zona [high[i-2], low[i]]
    Bearish: high[i] < low[i-2]  -> zona [high[i], low[i-2]]
    """
    out = []
    for i in range(2, len(df)):
        if df["low"].iloc[i] > df["high"].iloc[i - 2]:
            out.append((i, Direction.LONG, float(df["high"].iloc[i - 2]), float(df["low"].iloc[i])))
        elif df["high"].iloc[i] < df["low"].iloc[i - 2]:
            out.append((i, Direction.SHORT, float(df["high"].iloc[i]), float(df["low"].iloc[i - 2])))
    return out


def _fully_mitigated(df: pd.DataFrame, idx: int, lower: float, upper: float,
                     direction: Direction) -> bool:
    after = df["close"].iloc[idx + 1:]
    if after.empty:
        return False
    return bool((after < lower).any()) if direction is Direction.LONG else bool((after > upper).any())


def fvg(df: pd.DataFrame, cfg: Settings) -> SignalDraft | None:
    if len(df) < LOOKBACK:
        return None
    atr_val = float(atr_series(df, 14).iloc[-1])
    if atr_val <= 0:
        return None
    close = float(df["close"].iloc[-1])
    window_start = len(df) - LOOKBACK

    for i, direction, lower, upper in reversed(_gaps(df.iloc[window_start:])):
        idx = i + window_start
        if _fully_mitigated(df, idx, lower, upper, direction):
            continue
        mid = (lower + upper) / 2
        if direction is Direction.LONG:
            if close < lower or close - mid > atr_val * 1.5:
                continue
            structure = lower
        else:
            if close > upper or mid - close > atr_val * 1.5:
                continue
            structure = upper
        return SignalDraft(
            direction=direction,
            entry=round(mid, 6),
            structure_level=structure,
            core_strength=float(min(1.0, (upper - lower) / atr_val)),
            structure_event=StructureEvent.NONE,
            ob_zone=(lower, upper),
            note=f"{'bullish' if direction is Direction.LONG else 'bearish'} FVG "
                 f"{lower:.5f}-{upper:.5f}",
        )
    return None

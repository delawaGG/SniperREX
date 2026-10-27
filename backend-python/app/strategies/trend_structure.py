"""Trend Structure (spec §7.2): HH+HL / LH+LL + BOS trigger."""
from __future__ import annotations

import pandas as pd

from ..config import Settings
from ..indicators import atr as atr_series
from ..models.signal import Direction, StructureEvent
from .base import SignalDraft, swings

LOOKBACK, MIN_PATTERNS = 2, 2


def trend_structure(df: pd.DataFrame, cfg: Settings) -> SignalDraft | None:
    if len(df) < 40:
        return None
    atr_val = float(atr_series(df, 14).iloc[-1])
    if atr_val <= 0:
        return None

    highs, lows = swings(df, LOOKBACK)
    if len(highs) < MIN_PATTERNS + 1 or len(lows) < MIN_PATTERNS + 1:
        return None

    h, l = df["high"].to_numpy(), df["low"].to_numpy()
    hh = all(h[highs[-i]] > h[highs[-i - 1]] for i in range(1, MIN_PATTERNS + 1))
    hl = all(l[lows[-i]] > l[lows[-i - 1]] for i in range(1, MIN_PATTERNS + 1))
    lh = all(h[highs[-i]] < h[highs[-i - 1]] for i in range(1, MIN_PATTERNS + 1))
    ll = all(l[lows[-i]] < l[lows[-i - 1]] for i in range(1, MIN_PATTERNS + 1))
    close = float(df["close"].iloc[-1])
    last_swing_high, last_swing_low = float(h[highs[-1]]), float(l[lows[-1]])

    if hh and hl and close > last_swing_high:
        return SignalDraft(
            direction=Direction.LONG,
            entry=round(close, 6),
            structure_level=last_swing_low,
            core_strength=float(min(1.0, (close - last_swing_low) / (atr_val * 4))),
            structure_event=StructureEvent.BOS,
            note=f"HH+HL x{MIN_PATTERNS}, BOS {last_swing_high:.5f}",
        )
    if lh and ll and close < last_swing_low:
        return SignalDraft(
            direction=Direction.SHORT,
            entry=round(close, 6),
            structure_level=last_swing_high,
            core_strength=float(min(1.0, (last_swing_high - close) / (atr_val * 4))),
            structure_event=StructureEvent.BOS,
            note=f"LH+LL x{MIN_PATTERNS}, BOS {last_swing_low:.5f}",
        )
    return None

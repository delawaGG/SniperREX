"""EMA Cross (spec §7.1)."""
from __future__ import annotations

import pandas as pd

from ..config import Settings
from ..indicators import atr as atr_series, ema
from ..models.signal import Direction
from .base import SignalDraft

FAST, SLOW, SLOPE_WINDOW = 9, 21, 5


def ema_cross(df: pd.DataFrame, cfg: Settings) -> SignalDraft | None:
    if len(df) < SLOW + SLOPE_WINDOW + 2:
        return None
    fast = ema(df["close"], FAST)
    slow = ema(df["close"], SLOW)
    atr_val = float(atr_series(df, 14).iloc[-1])
    if atr_val <= 0:
        return None

    up_now, up_prev = fast.iloc[-1] > slow.iloc[-1], fast.iloc[-2] > slow.iloc[-2]
    down_now, down_prev = fast.iloc[-1] < slow.iloc[-1], fast.iloc[-2] < slow.iloc[-2]
    slope = float(slow.iloc[-1] - slow.iloc[-SLOPE_WINDOW])
    close = float(df["close"].iloc[-1])

    if up_now and not up_prev and slope > 0:
        direction = Direction.LONG
        structure = float(df["low"].iloc[-10:].min())
    elif down_now and not down_prev and slope < 0:
        direction = Direction.SHORT
        structure = float(df["high"].iloc[-10:].max())
    else:
        return None

    separation = abs(float(fast.iloc[-1] - slow.iloc[-1]))
    return SignalDraft(
        direction=direction,
        entry=round(close, 6),
        structure_level=structure,
        core_strength=min(1.0, separation / (atr_val * 0.5)),
        note=f"EMA{FAST}/{SLOW} cross, slope {slope:+.5f}",
    )

"""MACD Cross (spec §7.3)."""
from __future__ import annotations

import pandas as pd

from ..config import Settings
from ..indicators import atr as atr_series, ema
from ..models.signal import Direction
from .base import SignalDraft

FAST, SLOW, SIGNAL = 12, 26, 9


def macd_cross(df: pd.DataFrame, cfg: Settings) -> SignalDraft | None:
    if len(df) < SLOW + SIGNAL + 2:
        return None
    macd = ema(df["close"], FAST) - ema(df["close"], SLOW)
    signal_line = ema(macd, SIGNAL)
    hist = macd - signal_line
    atr_val = float(atr_series(df, 14).iloc[-1])
    if atr_val <= 0:
        return None

    cross_up = macd.iloc[-1] > signal_line.iloc[-1] and macd.iloc[-2] <= signal_line.iloc[-2]
    cross_dn = macd.iloc[-1] < signal_line.iloc[-1] and macd.iloc[-2] >= signal_line.iloc[-2]
    close = float(df["close"].iloc[-1])

    if cross_up and hist.iloc[-1] > 0:
        direction, structure = Direction.LONG, float(df["low"].iloc[-10:].min())
    elif cross_dn and hist.iloc[-1] < 0:
        direction, structure = Direction.SHORT, float(df["high"].iloc[-10:].max())
    else:
        return None

    return SignalDraft(
        direction=direction,
        entry=round(close, 6),
        structure_level=structure,
        core_strength=min(1.0, abs(float(hist.iloc[-1])) / atr_val),
        note=f"MACD cross, hist {float(hist.iloc[-1]):+.5f}",
    )

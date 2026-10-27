"""Order Block + BOS (spec §7.5) — LONG dan SHORT (mirror)."""
from __future__ import annotations

import pandas as pd

from ..config import Settings
from ..indicators import atr as atr_series
from ..models.signal import Direction, StructureEvent
from .base import SignalDraft, swings

SCAN = 80


def _last_swing_high(df: pd.DataFrame, window_start: int, lookback: int = 2) -> float | None:
    highs, _ = swings(df.iloc[window_start:], lookback)
    return float(df["high"].iloc[window_start + highs[-1]]) if highs else None


def _last_swing_low(df: pd.DataFrame, window_start: int, lookback: int = 2) -> float | None:
    _, lows = swings(df.iloc[window_start:], lookback)
    return float(df["low"].iloc[window_start + lows[-1]]) if lows else None


def order_block_bos(df: pd.DataFrame, cfg: Settings) -> SignalDraft | None:
    if len(df) < SCAN // 2:
        return None
    atr_val = float(atr_series(df, 14).iloc[-1])
    if atr_val <= 0:
        return None
    close = float(df["close"].iloc[-1])
    impulse = atr_val * 1.5
    window_start = len(df) - min(len(df), SCAN)

    for j in range(len(df) - 3, max(len(df) - 20, 0), -1):
        leg = df.iloc[j + 1:j + 4]
        if leg.empty:
            continue
        leg_high, leg_low = float(leg["high"].max()), float(leg["low"].min())
        low_j, high_j = float(df["low"].iloc[j]), float(df["high"].iloc[j])
        bullish_impulse = leg_high - low_j >= impulse
        bearish_impulse = high_j - leg_low >= impulse
        if not bullish_impulse and not bearish_impulse:
            continue

        direction = Direction.LONG if bullish_impulse else Direction.SHORT
        if bullish_impulse:
            ob_idx = next((k for k in range(j, max(j - 4, 0), -1)
                           if df["close"].iloc[k] < df["open"].iloc[k]), None)
        else:
            ob_idx = next((k for k in range(j, max(j - 4, 0), -1)
                           if df["close"].iloc[k] > df["open"].iloc[k]), None)
        if ob_idx is None:
            continue

        ob_low = float(df["low"].iloc[ob_idx])
        ob_high = float(df["high"].iloc[ob_idx])
        mid = (ob_low + ob_high) / 2

        if direction is Direction.LONG:
            bos = _last_swing_high(df, window_start)
            if bos is None or close <= ob_low or close > bos + atr_val * 1.5:
                continue
            event = StructureEvent.BOS if close > bos else StructureEvent.NONE
            strength = (leg_high - ob_low) / (atr_val * 3)
            structure = ob_low
        else:
            bos = _last_swing_low(df, window_start)
            if bos is None or close >= ob_high or close < bos - atr_val * 1.5:
                continue
            event = StructureEvent.BOS if close < bos else StructureEvent.NONE
            strength = (ob_high - leg_low) / (atr_val * 3)
            structure = ob_high

        return SignalDraft(
            direction=direction,
            entry=round(mid, 6),
            structure_level=structure,
            core_strength=float(min(1.0, strength)),
            structure_event=event,
            ob_zone=(ob_low, ob_high),
            note=f"OB {ob_low:.5f}-{ob_high:.5f} event={event.value}",
        )
    return None

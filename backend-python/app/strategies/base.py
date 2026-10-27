"""Kontrak strategi: pure function (df, cfg) -> SignalDraft | None."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol

import pandas as pd

from ..config import Settings
from ..models.signal import Direction, StructureEvent


@dataclass(frozen=True)
class SignalDraft:
    direction: Direction
    entry: float
    structure_level: float          # acuan SL (di bawah entry utk LONG)
    core_strength: float            # 0..1 -> bobot 30 poin
    structure_event: StructureEvent = StructureEvent.NONE
    ob_zone: tuple[float, float] | None = None
    note: str = ""


class Strategy(Protocol):
    def __call__(self, df: pd.DataFrame, cfg: Settings) -> SignalDraft | None: ...


def swings(df: pd.DataFrame, lookback: int = 2) -> tuple[list[int], list[int]]:
    """Fractal swing (satu swing per plateau, aman untuk double top/bottom)."""
    highs, lows = [], []
    h, l = df["high"].to_numpy(), df["low"].to_numpy()
    last_high_idx = last_low_idx = -10**9
    for i in range(lookback, len(df) - lookback):
        window_h = h[i - lookback:i + lookback + 1]
        window_l = l[i - lookback:i + lookback + 1]
        if h[i] == window_h.max() and i - last_high_idx > lookback:
            highs.append(i)
            last_high_idx = i
        if l[i] == window_l.min() and i - last_low_idx > lookback:
            lows.append(i)
            last_low_idx = i
    return highs, lows

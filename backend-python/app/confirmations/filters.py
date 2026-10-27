"""4 confirmation filters (spec §8). Semua return nilai aktual untuk logging."""
from __future__ import annotations

from dataclasses import dataclass

import pandas as pd

from ..config import Settings
from ..indicators import atr as atr_series, rsi as rsi_series, volume_ma
from ..models.signal import Confirmation, Direction
from ..symbols import atr_floor_for


@dataclass(frozen=True)
class FilterResult:
    name: Confirmation
    passed: bool
    value: float
    threshold: float
    detail: str


def rsi_filter(df: pd.DataFrame, direction: Direction, cfg: Settings) -> FilterResult:
    value = float(rsi_series(df, cfg.rsi_period).iloc[-1])
    if direction is Direction.LONG:
        passed, thr = value < cfg.rsi_overbought, cfg.rsi_overbought
    else:
        passed, thr = value > cfg.rsi_oversold, cfg.rsi_oversold
    return FilterResult(Confirmation.RSI, passed, round(value, 2), thr,
                        f"RSI {value:.1f} vs {thr:.0f}")


def atr_filter(df: pd.DataFrame, symbol: str, cfg: Settings) -> FilterResult:
    atr_val = float(atr_series(df, 14).iloc[-1])
    price = float(df["close"].iloc[-1])
    pct = atr_val / price if price else 0.0
    floor = atr_floor_for(symbol)          # v1.1: threshold per kategori
    return FilterResult(Confirmation.ATR, pct > floor, round(pct, 6), floor,
                        f"ATR {pct*100:.3f}% vs floor {floor*100:.3f}%")


def volume_filter(df: pd.DataFrame, cfg: Settings) -> FilterResult:
    ma = float(volume_ma(df, cfg.volume_ma_period).iloc[-1])
    vol = float(df["volume"].iloc[-1])
    ratio = vol / ma if ma > 0 else 0.0
    return FilterResult(Confirmation.VOLUME, ratio > cfg.volume_mult, round(ratio, 2),
                        cfg.volume_mult, f"vol/MA {ratio:.2f}x vs {cfg.volume_mult}x")


def ob_filter(df: pd.DataFrame, entry: float, cfg: Settings,
              zone: tuple[float, float] | None = None) -> FilterResult:
    """Ada OB dekat entry (± proximity ATR). Zona dari draft diprioritaskan."""
    atr_val = float(atr_series(df, 14).iloc[-1])
    proximity = atr_val * cfg.ob_proximity_atr
    if zone is not None:
        low, high = min(zone), max(zone)
        mid = (low + high) / 2
        dist = abs(entry - mid)
        return FilterResult(Confirmation.OB, dist <= proximity, round(dist, 6), proximity,
                            f"jarak ke OB {dist:.5f} (<= {proximity:.5f})")
    # fallback: candle berlawanan terakhir dalam 1 ATR
    for i in range(len(df) - 1, max(len(df) - 30, 0), -1):
        row = df.iloc[i]
        if row["close"] > row["open"]:            # bullish candle = potential demand
            mid = (row["low"] + row["high"]) / 2
            dist = abs(entry - mid)
            if dist <= proximity:
                return FilterResult(Confirmation.OB, True, round(dist, 6), proximity,
                                    f"OB fallback bar -{len(df)-1-i}, jarak {dist:.5f}")
    return FilterResult(Confirmation.OB, False, -1.0, proximity, "tidak ada OB dalam 1 ATR")


def run_filters(df: pd.DataFrame, symbol: str, direction: Direction, entry: float,
                selected: list[Confirmation], cfg: Settings,
                zone: tuple[float, float] | None = None) -> list[FilterResult]:
    out: list[FilterResult] = []
    for name in selected:
        if name is Confirmation.RSI:
            out.append(rsi_filter(df, direction, cfg))
        elif name is Confirmation.ATR:
            out.append(atr_filter(df, symbol, cfg))
        elif name is Confirmation.VOLUME:
            out.append(volume_filter(df, cfg))
        elif name is Confirmation.OB:
            out.append(ob_filter(df, entry, cfg, zone))
    return out

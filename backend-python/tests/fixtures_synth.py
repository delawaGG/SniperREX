"""Fixture sintetis deterministik untuk test strategi (pengganti CSV)."""
from __future__ import annotations

import pandas as pd


def make_df(candles: list[tuple[float, float, float, float]], start: str = "2026-10-01",
            volume: float = 1000.0) -> pd.DataFrame:
    idx = pd.date_range(start, periods=len(candles), freq="15min", tz="UTC")
    df = pd.DataFrame(candles, columns=["open", "high", "low", "close"], index=idx)
    df["volume"] = volume
    return df


def zigzag(n: int = 40, low: float = 99.0, high: float = 101.0) -> list[tuple]:
    """Plateau: high & low konstan (uji stabilitas deteksi swing)."""
    out = []
    mid = (low + high) / 2
    for i in range(n):
        if i % 2 == 0:
            out.append((mid, high, mid - 0.2, (mid + high) / 2))
        else:
            out.append((mid, mid + 0.2, low, (mid + low) / 2))
    return out


def rising_zigzag(n_pairs: int = 10, base_low: float = 99.0, base_high: float = 101.0,
                  step: float = 0.5) -> list[tuple]:
    """HH + HL berurutan (3 candle per leg agar swing unik & terdeteksi)."""
    out = []
    for k in range(n_pairs):
        lo = base_low + k * step
        hi = base_high + k * step
        out.append((lo + 0.1, hi, lo + 0.05, hi - 0.2))          # up -> swing high
        out.append((hi - 0.2, hi - 0.3, lo + 0.3, lo + 0.4))     # down
        out.append((lo + 0.4, lo + 0.45, lo - 0.05, lo + 0.1))   # down -> swing low
    return out


def falling_zigzag(n_pairs: int = 10, base_low: float = 99.0, base_high: float = 101.0,
                   step: float = 0.5) -> list[tuple]:
    """LH + LL berurutan (mirror dari rising_zigzag)."""
    out = []
    for k in range(n_pairs):
        lo = base_low - k * step
        hi = base_high - k * step
        out.append((hi - 0.1, hi - 0.05, lo, lo + 0.2))          # down -> swing low
        out.append((lo + 0.2, lo + 0.35, lo + 0.15, lo + 0.3))   # up
        out.append((lo + 0.3, hi, hi - 0.2, hi - 0.15))          # up -> swing high
    return out


def bull_trend(n: int = 60, base: float = 100.0, step: float = 0.05):
    out = []
    price = base
    for _ in range(n):
        o = price
        c = price + step
        out.append((o, c + 0.1, o - 0.1, c))
        price = c
    return out


def bear_trend(n: int = 60, base: float = 120.0, step: float = 0.05):
    out = []
    price = base
    for _ in range(n):
        o = price
        c = price - step
        out.append((o, o + 0.1, c - 0.1, c))
        price = c
    return out


def downtrend_then_rally(n_down: int = 40, n_up: int = 3, base: float = 120.0,
                         rally: float = 2.0):
    out = []
    price = base
    for _ in range(n_down):
        o, c = price, price - 0.2
        out.append((o, o + 0.05, c - 0.05, c))
        price = c
    for _ in range(n_up):
        o, c = price, price + rally
        out.append((o, c + 0.1, o - 0.05, c))
        price = c
    return out


def fvg_bullish_setup(flat_n: int = 90):
    out = []
    price = 100.0
    for _ in range(flat_n):
        out.append((price, price + 0.2, price - 0.2, price))
    out.append((100.0, 100.2, 99.9, 100.1))     # candle A: high 100.2
    out.append((100.1, 101.4, 100.0, 101.3))    # candle B: impulse
    out.append((101.3, 101.5, 100.5, 101.0))    # candle C: low 100.5 -> gap [100.2, 100.5]
    out.append((101.0, 101.1, 100.55, 100.8))   # retest ringan
    out.append((100.8, 100.9, 100.5, 100.7))
    return out


def ob_bos_setup(zigzag_n: int = 40):
    """Swing -> down candle (OB) -> impulse bullish -> close break swing high."""
    out = zigzag(zigzag_n, 99.0, 101.0)
    out.append((100.2, 100.4, 99.4, 99.5))      # OB bearish
    out.append((99.5, 101.0, 99.4, 100.9))      # impulse 1
    out.append((100.9, 102.4, 100.8, 102.3))    # impulse 2
    out.append((102.3, 102.6, 102.0, 102.5))    # close > swing high
    return out


def fvg_bearish_setup(flat_n: int = 90):
    """Flat, lalu gap bearish 99.5-99.8, retest ringan ke 50%."""
    out = []
    price = 100.0
    for _ in range(flat_n):
        out.append((price, price + 0.2, price - 0.2, price))
    out.append((100.0, 100.1, 99.8, 99.9))      # candle A: low 99.8
    out.append((99.9, 100.0, 98.6, 98.7))       # impulse turun
    out.append((98.7, 99.5, 98.5, 99.2))        # candle C: high 99.5 -> gap [99.5, 99.8]
    out.append((99.2, 99.45, 99.1, 99.3))       # retest, close < 99.8
    out.append((99.3, 99.6, 99.15, 99.4))
    return out


def ob_bos_short_setup(zigzag_n: int = 40):
    """Swing -> up candle (OB) -> impulse bearish -> close break swing low."""
    out = zigzag(zigzag_n, 99.0, 101.0)
    out.append((99.8, 100.6, 99.7, 100.5))      # OB bullish
    out.append((100.5, 100.6, 99.0, 99.1))      # impulse 1
    out.append((99.1, 99.2, 97.6, 97.7))        # impulse 2
    out.append((97.7, 97.9, 97.4, 97.5))        # close < swing low
    return out

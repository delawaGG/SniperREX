"""Katalog pair + metadata pip/point (v1.1: pip != point).

`td_symbol` = format yang diterima Twelve Data (mis. XAU/USD, bukan XAUUSD).
"""
from __future__ import annotations

from .models.signal import PairInfo

PAIRS: dict[str, PairInfo] = {
    "XAUUSD": PairInfo(symbol="XAUUSD", td_symbol="XAU/USD", name="Gold Spot / US Dollar",
                       category="Metal", pip_size=0.1, point_size=0.01, digits=2),
    "EURUSD": PairInfo(symbol="EURUSD", td_symbol="EUR/USD", name="Euro / US Dollar",
                       category="Forex", pip_size=0.0001, point_size=0.00001, digits=5),
    "GBPUSD": PairInfo(symbol="GBPUSD", td_symbol="GBP/USD", name="British Pound / US Dollar",
                       category="Forex", pip_size=0.0001, point_size=0.00001, digits=5),
    "USDJPY": PairInfo(symbol="USDJPY", td_symbol="USD/JPY", name="US Dollar / Japanese Yen",
                       category="Forex", pip_size=0.01, point_size=0.001, digits=3),
    "BTCUSD": PairInfo(symbol="BTCUSD", td_symbol="BTC/USD", name="Bitcoin / US Dollar",
                       category="Crypto", pip_size=1.0, point_size=0.01, digits=2),
}

# Kategori volatilitas untuk threshold ATR (v1.1 gap #9): pair JPY butuh
# threshold lebih rendah karena ATR absolutnya jauh lebih kecil dari EURUSD.
ATR_PCT_FLOOR: dict[str, float] = {
    "Metal": 0.0005,
    "Forex": 0.0003,
    "Forex_JPY": 0.0002,
    "Crypto": 0.0010,
}


def pair_info(symbol: str) -> PairInfo | None:
    return PAIRS.get(symbol.upper())


def atr_floor_for(symbol: str) -> float:
    sym = symbol.upper()
    if sym.endswith("JPY"):
        return ATR_PCT_FLOOR["Forex_JPY"]
    info = PAIRS.get(sym)
    return ATR_PCT_FLOOR.get(info.category, 0.0005) if info else 0.0005

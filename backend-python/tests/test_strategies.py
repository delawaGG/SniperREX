from __future__ import annotations

import pytest

from app.config import Settings
from app.confirmations.filters import atr_filter, ob_filter, rsi_filter, run_filters, volume_filter
from app.models.signal import Confirmation, Direction
from app.strategies.ema_cross import ema_cross
from app.strategies.fvg import fvg
from app.strategies.macd_cross import macd_cross
from app.strategies.ob_bos import order_block_bos
from app.strategies.trend_structure import trend_structure

from .fixtures_synth import (
    bull_trend,
    fvg_bearish_setup,
    ob_bos_short_setup,
    downtrend_then_rally,
    falling_zigzag,
    fvg_bullish_setup,
    make_df,
    ob_bos_setup,
    rising_zigzag,
    zigzag,
)

CFG = Settings()


def _valid(draft) -> bool:
    if draft is None:
        return False
    if draft.direction is Direction.LONG:
        return draft.structure_level < draft.entry
    return draft.structure_level > draft.entry


# ---------------- FVG ----------------
def test_fvg_detects_bullish_gap_and_retest_half():
    draft = fvg(make_df(fvg_bullish_setup()), CFG)
    assert draft is not None
    assert draft.direction is Direction.LONG
    assert draft.entry == pytest.approx(100.35, abs=1e-6)      # 50% dari 100.2-100.5
    assert draft.structure_level == pytest.approx(100.2, abs=1e-6)  # SL di bawah gap
    assert 0 < draft.core_strength <= 1
    assert _valid(draft)


def test_fvg_rejects_fully_mitigated_gap():
    candles = fvg_bullish_setup() + [(100.7, 100.8, 99.0, 99.2)]
    assert fvg(make_df(candles), CFG) is None


def test_fvg_bearish_mirror():
    draft = fvg(make_df(fvg_bearish_setup()), CFG)
    assert draft is not None
    assert draft.direction is Direction.SHORT
    assert draft.entry == pytest.approx(99.65, abs=1e-6)      # 50% dari 99.5-99.8
    assert draft.structure_level == pytest.approx(99.8, abs=1e-6)
    assert _valid(draft)


def test_fvg_none_on_flat_market():
    assert fvg(make_df([(100, 100.2, 99.8, 100) for _ in range(80)]), CFG) is None


# ---------------- Order Block + BOS ----------------
def test_ob_bos_long_setup():
    draft = order_block_bos(make_df(ob_bos_setup()), CFG)
    assert draft is not None
    assert draft.direction is Direction.LONG
    assert draft.structure_event.value == "BOS"
    assert draft.ob_zone is not None and draft.ob_zone[0] < draft.ob_zone[1]
    assert _valid(draft)


def test_ob_bos_short_mirror():
    draft = order_block_bos(make_df(ob_bos_short_setup()), CFG)
    assert draft is not None
    assert draft.direction is Direction.SHORT
    assert draft.structure_level > draft.entry      # SL di atas entry
    assert _valid(draft)


def test_ob_bos_none_without_impulse():
    assert order_block_bos(make_df([(100, 100.3, 99.8, 100) for _ in range(80)]), CFG) is None


# ---------------- EMA / MACD ----------------
def test_ema_cross_long_after_rally():
    draft = ema_cross(make_df(downtrend_then_rally()), CFG)
    assert draft is not None and draft.direction is Direction.LONG
    assert _valid(draft)


def test_ema_cross_none_on_steady_trend_without_cross():
    assert ema_cross(make_df(bull_trend(80)), CFG) is None


def test_macd_cross_fires_on_some_bar_after_reversal():
    """Cross harus terjadi tepat di bar terakhir -> uji beberapa panjang rally."""
    drafts = [macd_cross(make_df(downtrend_then_rally(n_down=45, n_up=n)), CFG)
              for n in range(1, 10)]
    found = [d for d in drafts if d is not None]
    assert found, "tidak ada cross terdeteksi pada seluruh variasi rally"
    assert all(d.direction is Direction.LONG and _valid(d) for d in found)


def test_macd_none_on_flat():
    assert macd_cross(make_df([(100, 100.2, 99.8, 100) for _ in range(80)]), CFG) is None


# ---------------- Trend Structure ----------------
def test_trend_structure_requires_bos():
    assert trend_structure(make_df(rising_zigzag(15)), CFG) is None   # HH+HL tanpa break


def test_trend_structure_long_on_break():
    draft = trend_structure(make_df(rising_zigzag(15) + [(110.0, 110.9, 109.9, 110.8)]), CFG)
    assert draft is not None
    assert draft.direction is Direction.LONG
    assert draft.structure_event.value == "BOS"
    assert _valid(draft)


def test_trend_structure_short_on_break():
    draft = trend_structure(make_df(falling_zigzag(15) + [(90.0, 90.1, 89.0, 89.2)]), CFG)
    assert draft is not None
    assert draft.direction is Direction.SHORT
    assert _valid(draft)


def test_trend_structure_none_on_random_zigzag():
    assert trend_structure(make_df(zigzag(40)), CFG) is None


# ---------------- Confirmations ----------------
def test_rsi_filter_blocks_overbought_long():
    res = rsi_filter(make_df(bull_trend(60)), Direction.LONG, CFG)
    assert res.name is Confirmation.RSI
    assert res.value == 100.0        # tren naik murni -> RSI 100, bukan NaN
    assert res.passed is False
    assert rsi_filter(make_df(bull_trend(60)), Direction.SHORT, CFG).passed is True


def test_atr_filter_uses_symbol_category():
    flat_eur = make_df([(1.0800, 1.08002, 1.07998, 1.08) for _ in range(60)])
    assert atr_filter(flat_eur, "EURUSD", CFG).passed is False
    flat_jpy = make_df([(150.00, 150.02, 149.98, 150) for _ in range(60)])
    assert atr_filter(flat_jpy, "USDJPY", CFG).passed is True   # floor JPY lebih rendah
    active = make_df(bull_trend(60, 1.0800, 0.0020))
    assert atr_filter(active, "EURUSD", CFG).passed is True


def test_volume_filter_ratio():
    df = make_df([(100, 100.2, 99.8, 100) for _ in range(30)])
    df.loc[df.index[-1], "volume"] = 5000
    assert volume_filter(df, CFG).passed is True


def test_ob_filter_with_zone_and_without():
    df = make_df([(100, 100.2, 99.8, 100) for _ in range(40)])
    assert ob_filter(df, 100.1, CFG, zone=(99.9, 100.2)).passed is True
    assert ob_filter(df, 130.0, CFG, zone=(99.9, 100.2)).passed is False


def test_run_filters_returns_all_selected():
    results = run_filters(make_df(ob_bos_setup()), "XAUUSD", Direction.LONG, 100.0,
                          [Confirmation.RSI, Confirmation.ATR, Confirmation.OB], CFG)
    assert [r.name for r in results] == [Confirmation.RSI, Confirmation.ATR, Confirmation.OB]
    assert all(r.detail for r in results)

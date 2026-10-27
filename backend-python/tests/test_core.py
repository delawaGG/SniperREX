from __future__ import annotations

import asyncio

import pytest

from app.config import Settings
from app.models.signal import (
    Confluence,
    CoreStrategy,
    Direction,
    Signal,
    SignalContext,
    SignalStatus,
)
from app.risk.levels import calculate_levels
from app.scoring.confluence_score import quant_score, required_threshold
from app.store.memory_store import SignalStore

CFG = Settings()


def _levels(direction: Direction, entry: float, structure: float, atr: float,
            point: float, pip: float):
    return calculate_levels(direction=direction, entry=entry, structure_level=structure,
                            atr=atr, point_size=point, pip_size=pip, cfg=CFG)


def test_levels_long_xauusd():
    lv = _levels(Direction.LONG, 2650.50, 2645.00, 3.20, 0.01, 0.1)
    assert lv.stop_loss == pytest.approx(2644.36, abs=1e-6)   # 2645 - 0.2*3.2
    assert lv.rr == 2.0                                        # v1.1: RR basis TP1
    assert lv.rr_extension == 4.0                              # TP3
    assert len(lv.take_profit) == 3
    assert lv.take_profit[0] > lv.entry


def test_levels_short_eurusd_rounding():
    lv = _levels(Direction.SHORT, 1.08500, 1.08700, 0.00120, 0.00001, 0.0001)
    assert lv.stop_loss > lv.entry
    assert lv.take_profit[0] < lv.entry
    # pembulatan mengikuti point_size EURUSD = 0.00001 (kelipatan 1e-5)
    for value in (lv.entry, lv.stop_loss, *lv.take_profit):
        assert round(value / 0.00001) == pytest.approx(value / 0.00001, abs=1e-6)


def test_levels_reject_zero_risk():
    with pytest.raises(ValueError):
        _levels(Direction.LONG, 2650.0, 2650.0, 0.0, 0.01, 0.1)


def test_threshold_hybrid():
    assert required_threshold(4, CFG) == 3      # fixed 3/4
    assert required_threshold(3, CFG) == 2      # proporsional ceil(0.66*3)=2
    assert required_threshold(2, CFG) == 2      # minimal 2
    assert required_threshold(1, CFG) == 2      # tidak pernah 0


def test_score_bounds_and_weights():
    best = quant_score(core_strength=1.0, confirmations_passed=4, confirmations_selected=4,
                       rr_extension=4.0, trend_alignment="aligned", atr_pct=0.001, cfg=CFG)
    assert best.total == 100
    worst = quant_score(core_strength=0.0, confirmations_passed=0, confirmations_selected=4,
                        rr_extension=1.0, trend_alignment="counter", atr_pct=0.05, cfg=CFG)
    assert worst.total == 5       # hanya komponen RR minimal
    assert 0 <= worst.total <= 100


def _signal(created_minute: int = 0) -> Signal:
    from datetime import datetime, timedelta, timezone
    ts = datetime(2026, 10, 27, 10, created_minute, tzinfo=timezone.utc)
    return Signal(
        pair="XAUUSD", timeframe="M15", direction=Direction.LONG,
        core_strategy=CoreStrategy.FVG, confirmations=[],
        entry=2650.5, stop_loss=2645.0, take_profit=[2661.5, 2667.0, 2672.5],
        risk_reward=2.0, quant_score=78,
        confluence=Confluence(core_passed=True, rsi_passed=True, atr_passed=True,
                             volume_passed=False, ob_passed=True,
                             total_confluence=3, threshold=3),
        context=SignalContext(current_price=2650.5, atr=3.2, rsi=42.5,
                             trend="BULLISH", session="LONDON"),
        created_at=ts,
    )


def test_store_dedupe_same_bar():
    store = SignalStore(ttl_sec=600, grace_sec=90)

    async def run():
        s1 = _signal(0)
        got1, created1 = await store.add(s1)
        got2, created2 = await store.add(_signal(0))
        return got1, created1, got2, created2

    got1, created1, got2, created2 = asyncio.run(run())
    assert created1 is True
    assert created2 is False
    assert got2.id == got1.id      # duplikat -> kembalikan signal lama


def test_store_status_update_only():
    store = SignalStore(ttl_sec=600, grace_sec=90)

    async def run():
        await store.add(_signal(1))
        sig = (await store.list())[0]
        updated = await store.update_status(sig.id, SignalStatus.SKIPPED)
        return sig, updated

    original, updated = asyncio.run(run())
    assert updated is not None and updated.status is SignalStatus.SKIPPED
    assert original.status is SignalStatus.WAIT          # objek asli tidak termutasi
    assert updated.entry == original.entry

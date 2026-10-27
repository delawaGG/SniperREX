"""E2E scan dengan Twelve Data di-mock (tanpa API key, tanpa network)."""
from __future__ import annotations

import asyncio

import httpx
import pytest

from app.clients.twelve_data_client import TwelveDataClient
from app.config import Settings
from app.models.signal import Confirmation, CoreStrategy, Direction, ScanRequest, SignalStatus
from app.services.scan_service import ScanError, ScanService
from app.store.memory_store import SignalStore

from .fixtures_synth import fvg_bullish_setup, make_df


def _payload(df) -> list[dict]:
    return [
        {
            "datetime": idx.strftime("%Y-%m-%d %H:%M:%S"),
            "open": str(row.open), "high": str(row.high),
            "low": str(row.low), "close": str(row.close),
            "volume": str(row.volume),
        }
        for idx, row in df.iterrows()
    ]


def _handler(df, plan: str = "free"):
    def handler(request: httpx.Request) -> httpx.Response:
        if "api_usage" in request.url.path:
            return httpx.Response(200, json={"plan_category": plan, "current_usage": 12})
        if "time_series" in request.url.path:
            return httpx.Response(200, json={
                "meta": {"symbol": "XAUUSD", "interval": request.url.params.get("interval")},
                "values": _payload(df),
                "status": "ok",
            })
        return httpx.Response(404, json={"status": "error", "message": "not found"})
    return handler


def _service(df, plan: str = "free") -> ScanService:
    cfg = Settings(twelve_data_api_key="test-key")
    client = TwelveDataClient(cfg, transport=httpx.MockTransport(_handler(df, plan)))
    return ScanService(client, SignalStore(ttl_sec=600, grace_sec=90), cfg)


REQ = ScanRequest(pair="XAUUSD", timeframe="M15", core_strategy=CoreStrategy.FVG,
                  confirmations=[Confirmation.RSI, Confirmation.ATR, Confirmation.OB])


def test_scan_produces_signal_from_live_feed():
    df = make_df(fvg_bullish_setup())
    outcome = asyncio.run(_service(df).run(REQ))

    assert outcome.reason is None, outcome.reason
    sig = outcome.signal
    assert sig is not None
    assert sig.direction is Direction.LONG
    assert sig.pair == "XAUUSD" and sig.timeframe == "M15"
    # RR basis TP1 = 2.0 selalu
    assert sig.risk_reward == pytest.approx(2.0)
    assert len(sig.take_profit) == 3
    assert sig.stop_loss < sig.entry < sig.take_profit[0]
    # TP1 = entry + 2 * (entry - SL)
    risk = sig.entry - sig.stop_loss
    assert sig.take_profit[0] == pytest.approx(sig.entry + 2 * risk, abs=1e-6)
    assert sig.quant_score > 0
    assert sig.confluence.threshold == 2      # 3 filter -> proporsional, bukan 3/3
    assert sig.status is SignalStatus.WAIT


def test_scan_is_deterministic_and_deduped():
    df = make_df(fvg_bullish_setup())
    svc = _service(df)

    async def run():
        a = await svc.run(REQ)
        b = await svc.run(REQ)
        return a, b

    first, second = asyncio.run(run())
    assert first.signal is not None and second.signal is not None
    assert first.signal.id == second.signal.id
    assert first.signal.entry == second.signal.entry
    assert any("identik" in w for w in second.warnings)


def test_free_tier_blocks_h1():
    svc = _service(make_df(fvg_bullish_setup()))
    req = REQ.model_copy(update={"timeframe": "H1"})
    with pytest.raises(ScanError) as exc:
        asyncio.run(svc.run(req))
    assert exc.value.status_code == 403


def test_pro_tier_allows_h4():
    svc = _service(make_df(fvg_bullish_setup()), plan="grow")
    out = asyncio.run(svc.run(REQ.model_copy(update={"timeframe": "H4"})))
    assert out.reason is not None or out.signal is not None


def test_unknown_strategy_setup_returns_no_setup():
    svc = _service(make_df([(100, 100.2, 99.8, 100) for _ in range(80)]))
    out = asyncio.run(svc.run(REQ))
    assert out.signal is None and out.reason == "no_setup"

"""Smoke test feed nyata — tidak pernah mencetak API key.

Budget FREE = 8 kredit/menit. Skrip ini sengaja hemat: 5-6 request.
"""
import asyncio
import sys

sys.path.insert(0, "/var/minis/workspace/sniperrex/backend-python")

from app.clients.twelve_data_client import TwelveDataClient, TwelveDataError
from app.config import get_settings
from app.models.signal import Confirmation, CoreStrategy, ScanRequest
from app.services.scan_service import ScanError, ScanService
from app.store.memory_store import SignalStore
from app.symbols import pair_info


async def main() -> None:
    cfg = get_settings()
    print("key loaded:", bool(cfg.twelve_data_api_key))
    client = TwelveDataClient(cfg)

    try:
        tier = await client.detect_tier()
        print("tier:", tier["tier"], "| plan:", tier["plan_raw"],
              "| limit/menit:", tier["usage"].get("plan_limit"))
    except TwelveDataError as exc:
        print("detect_tier gagal:", exc, "| code:", exc.code)
        return

    for pair in ("XAUUSD", "EURUSD"):
        td = pair_info(pair).td_symbol
        try:
            df = await client.fetch_ohlcv(td, "M15", 300)
            print(f"{pair} ({td}) M15: {len(df)} candle | terakhir {df.index[-1]} close {df['close'].iloc[-1]:.5f}")
        except TwelveDataError as exc:
            print(f"{pair} M15: ERROR {exc} | code {exc.code}")

    svc = ScanService(client, SignalStore(), cfg)
    for strategy in (CoreStrategy.FVG, CoreStrategy.ORDER_BLOCK_BOS):
        req = ScanRequest(pair="XAUUSD", timeframe="M15", core_strategy=strategy,
                          confirmations=[Confirmation.RSI, Confirmation.ATR, Confirmation.OB])
        try:
            out = await svc.run(req)
        except ScanError as exc:
            print(f"scan {strategy.value}: ditolak -> {exc}")
            continue
        if out.signal:
            s = out.signal
            print(f"scan {strategy.value}: SIGNAL {s.direction.value} entry {s.entry} sl {s.stop_loss} "
                  f"tp {s.take_profit} rr {s.risk_reward} score {s.quant_score} "
                  f"konfluensi {s.confluence.total_confluence}/{s.confluence.threshold}")
        else:
            print(f"scan {strategy.value}: {out.reason}")
        for w in out.warnings[:4]:
            print("   -", w)


asyncio.run(main())

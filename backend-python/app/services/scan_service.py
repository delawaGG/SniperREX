"""Orkestrasi scan: fetch -> strategi -> konfirmasi -> risk -> score -> store."""
from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import datetime, timezone

import pandas as pd

from ..clients.twelve_data_client import TwelveDataClient, TwelveDataError
from ..config import Settings
from ..confirmations.filters import FilterResult, run_filters
from ..indicators import atr, ema, rsi
from ..models.signal import (
    Confluence,
    CoreStrategy,
    Direction,
    ScanRequest,
    Signal,
    SignalContext,
)
from ..risk.levels import calculate_levels
from ..scoring.confluence_score import quant_score, required_threshold
from ..strategies import ema_cross as ema_strat
from ..strategies import fvg as fvg_strat
from ..strategies import macd_cross as macd_strat
from ..strategies import ob_bos as ob_strat
from ..strategies import trend_structure as trend_strat
from ..strategies.base import SignalDraft
from ..store.memory_store import SignalStore
from ..symbols import atr_floor_for, pair_info

log = logging.getLogger(__name__)

SESSIONS = ((0, 7, "ASIA"), (7, 12, "LONDON"), (12, 21, "NEW_YORK"), (21, 24, "SYDNEY"))

STRATEGIES = {
    CoreStrategy.FVG: fvg_strat.fvg,
    CoreStrategy.ORDER_BLOCK_BOS: ob_strat.order_block_bos,
    CoreStrategy.EMA_CROSS: ema_strat.ema_cross,
    CoreStrategy.MACD_CROSS: macd_strat.macd_cross,
    CoreStrategy.TREND_STRUCTURE: trend_strat.trend_structure,
}


class ScanError(RuntimeError):
    def __init__(self, message: str, status_code: int = 400, warnings: list[str] | None = None):
        super().__init__(message)
        self.status_code = status_code
        self.warnings = warnings or []


@dataclass
class ScanOutcome:
    signal: Signal | None
    reason: str | None = None
    warnings: list[str] = field(default_factory=list)
    context: SignalContext | None = None
    filters: list[FilterResult] = field(default_factory=list)


def session_of(moment: datetime) -> str:
    hour = moment.astimezone(timezone.utc).hour
    for start, end, name in SESSIONS:
        if start <= hour < end:
            return name
    return "OFF"


def build_context(df: pd.DataFrame, cfg: Settings) -> SignalContext:
    fast = float(ema(df["close"], 21).iloc[-1])
    slow = float(ema(df["close"], 55).iloc[-1])
    return SignalContext(
        current_price=float(df["close"].iloc[-1]),
        atr=round(float(atr(df, 14).iloc[-1]), 6),
        rsi=round(float(rsi(df, cfg.rsi_period).iloc[-1]), 2),
        trend="BULLISH" if fast > slow else "BEARISH",
        session=session_of(datetime.now(timezone.utc)),
    )


def trend_alignment(df_htf: pd.DataFrame, direction: Direction) -> str:
    fast = float(ema(df_htf["close"], 21).iloc[-1])
    slow = float(ema(df_htf["close"], 55).iloc[-1])
    htf_up = fast > slow
    if (direction is Direction.LONG and htf_up) or (direction is Direction.SHORT and not htf_up):
        return "aligned"
    return "counter"


class ScanService:
    def __init__(self, client: TwelveDataClient, store: SignalStore, cfg: Settings) -> None:
        self._client = client
        self._store = store
        self._cfg = cfg

    async def allowed_timeframes(self) -> list[str]:
        try:
            tier = (await self._client.detect_tier())["tier"]
        except TwelveDataError:
            tier = "FREE"
        return list(self._cfg.pro_timeframes if tier == "PRO" else self._cfg.free_timeframes)

    async def run(self, req: ScanRequest) -> ScanOutcome:
        info = pair_info(req.pair)
        if not info:
            raise ScanError(f"pair tidak dikenal: {req.pair}", 400)

        tf = req.timeframe.upper()
        allowed = await self.allowed_timeframes()
        if tf not in allowed:
            raise ScanError(
                f"timeframe {tf} tidak tersedia untuk tier kamu. Pilihan: {', '.join(allowed)}",
                403, warnings=["Tier dideteksi otomatis dari Twelve Data"],
            )

        df = await self._client.fetch_ohlcv(info.td_symbol, tf, outputsize=300)
        if len(df) < 60:
            raise ScanError(f"data {info.symbol} {tf} kurang dari 60 candle", 422)
        ctx = build_context(df, self._cfg)

        is_free = tf in self._cfg.free_timeframes
        htf_tf = self._cfg.free_htf_proxy if is_free else self._cfg.pro_htf_timeframes[0]
        htf_warnings: list[str] = []
        try:
            htf_df = await self._client.fetch_ohlcv(info.td_symbol, htf_tf, outputsize=200)
        except TwelveDataError as exc:
            htf_df = None
            htf_warnings.append(f"HTF {htf_tf} gagal diambil ({exc}) — skor trend = mixed")
            log.warning("HTF fetch gagal: %s", exc)

        draft: SignalDraft | None = STRATEGIES[req.core_strategy](df, self._cfg)
        if draft is None:
            return ScanOutcome(None, "no_setup", htf_warnings + [
                f"strategi {req.core_strategy.value} tidak menemukan setup valid di bar terakhir",
                f"HTF {htf_tf} dipakai untuk alignment",
            ], ctx)

        selected = list(req.confirmations)
        filters = run_filters(df, info.symbol, draft.direction, draft.entry, selected,
                             self._cfg, zone=draft.ob_zone)
        passed = sum(f.passed for f in filters)
        threshold = required_threshold(len(selected), self._cfg)
        warnings = htf_warnings + [f"{f.name.value}:{'PASS' if f.passed else 'FAIL'} {f.detail}" for f in filters]

        if passed < threshold:
            return ScanOutcome(None, f"konfluensi {passed}/{len(selected)} < gate {threshold}",
                               warnings, ctx, filters)

        try:
            levels = calculate_levels(
                direction=draft.direction,
                entry=draft.entry,
                structure_level=draft.structure_level,
                atr=float(atr(df, 14).iloc[-1]),
                point_size=info.point_size,
                pip_size=info.pip_size,
                cfg=self._cfg,
            )
        except ValueError as exc:
            return ScanOutcome(None, f"level tidak valid: {exc}", warnings, ctx, filters)

        alignment = trend_alignment(htf_df, draft.direction) if htf_df is not None else "mixed"
        atr_pct = ctx.atr / ctx.current_price if ctx.current_price else 0.0
        score = quant_score(
            core_strength=draft.core_strength,
            confirmations_passed=passed,
            confirmations_selected=len(selected),
            rr_extension=levels.rr_extension,
            trend_alignment=alignment,
            atr_pct=atr_pct,
            cfg=self._cfg,
        )
        if score.total < 50:
            warnings.append(f"quant_score rendah ({score.total}) — pertimbangkan SKIP")
        if atr_pct < atr_floor_for(info.symbol):
            warnings.append("volatilitas di bawah floor: pasar cenderung flat")

        names = {f.name for f in filters if f.passed}
        signal = Signal(
            pair=info.symbol,
            timeframe=tf,
            direction=draft.direction,
            core_strategy=req.core_strategy,
            structure_event=draft.structure_event,
            confirmations=[c for c in selected if c in names],
            entry=levels.entry,
            stop_loss=levels.stop_loss,
            take_profit=list(levels.take_profit),
            risk_reward=levels.rr,
            quant_score=score.total,
            confluence=Confluence(
                core_passed=True,
                rsi_passed=any(f.name.value == "RSI" and f.passed for f in filters),
                atr_passed=any(f.name.value == "ATR" and f.passed for f in filters),
                volume_passed=any(f.name.value == "VOLUME" and f.passed for f in filters),
                ob_passed=any(f.name.value == "OB" and f.passed for f in filters),
                total_confluence=passed,
                threshold=threshold,
            ),
            context=ctx,
        )
        stored, created = await self._store.add(signal)
        if not created:
            warnings.append("signal identik sudah ada di bar ini — mengembalikan yang lama")
        return ScanOutcome(stored, None, warnings, ctx, filters)

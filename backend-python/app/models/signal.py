"""Pydantic v2 models — schema Signal (read-only setelah dibuat)."""
from __future__ import annotations

from datetime import datetime, timezone
from enum import Enum
from typing import Any
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field, computed_field


class Direction(str, Enum):
    LONG = "LONG"
    SHORT = "SHORT"


class CoreStrategy(str, Enum):
    FVG = "FVG"
    ORDER_BLOCK_BOS = "ORDER_BLOCK_BOS"
    EMA_CROSS = "EMA_CROSS"
    TREND_STRUCTURE = "TREND_STRUCTURE"
    MACD_CROSS = "MACD_CROSS"


class Confirmation(str, Enum):
    RSI = "RSI"
    ATR = "ATR"
    VOLUME = "VOLUME"
    OB = "OB"


class StructureEvent(str, Enum):
    """v1.1: CHOCH dipisah dari strategy enum (spec §7.5 mendeteksi CHOCH)."""

    BOS = "BOS"
    CHOCH = "CHOCH"
    NONE = "NONE"


class SignalStatus(str, Enum):
    WAIT = "WAIT"
    ACTIVE = "ACTIVE"
    HIT_TP = "HIT_TP"
    HIT_SL = "HIT_SL"
    SKIPPED = "SKIPPED"


class Verdict(str, Enum):
    TAKE = "TAKE"
    WAIT = "WAIT"
    SKIP = "SKIP"


class Confluence(BaseModel):
    model_config = ConfigDict(frozen=True)
    core_passed: bool
    rsi_passed: bool
    atr_passed: bool
    volume_passed: bool
    ob_passed: bool
    total_confluence: int
    threshold: int


class SignalContext(BaseModel):
    model_config = ConfigDict(frozen=True)
    current_price: float
    atr: float
    rsi: float
    trend: str
    session: str


class Signal(BaseModel):
    """Signal immutable: hanya `status` yang boleh berubah lewat endpoint status."""

    model_config = ConfigDict(frozen=True)

    id: str = Field(default_factory=lambda: str(uuid4()))
    pair: str
    timeframe: str
    direction: Direction
    core_strategy: CoreStrategy
    structure_event: StructureEvent = StructureEvent.NONE
    confirmations: list[Confirmation]
    entry: float
    stop_loss: float
    take_profit: list[float]
    risk_reward: float          # v1.1: basis TP1 (spec §9)
    quant_score: int
    confluence: Confluence
    context: SignalContext
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    status: SignalStatus = SignalStatus.WAIT

    @computed_field  # type: ignore[prop-decorator]
    @property
    def dedupe_key(self) -> str:
        """Cegah duplikat signal pada bar yang sama (v1.1 blocker #7)."""
        bar = self.created_at.replace(second=0, microsecond=0)
        return f"{self.pair}:{self.timeframe}:{self.core_strategy.value}:{self.direction.value}:{bar.isoformat()}"


class SignalEnvelope(BaseModel):
    """Wrapper root-level — disclaimer wajib ada di level root (spec §14.8)."""

    disclaimer: str
    signal: Signal | None = None
    reason: str | None = None
    warnings: list[str] = Field(default_factory=list)


class SignalListEnvelope(BaseModel):
    disclaimer: str
    signals: list[Signal] = Field(default_factory=list)
    count: int = 0


class ScanRequest(BaseModel):
    pair: str
    timeframe: str
    core_strategy: CoreStrategy
    confirmations: list[Confirmation] = Field(
        default_factory=lambda: [
            Confirmation.RSI, Confirmation.ATR, Confirmation.OB
        ]
    )


class StatusUpdate(BaseModel):
    status: SignalStatus


class AiVerdict(BaseModel):
    signal_id: str
    ai_score: int = Field(ge=0, le=100)
    verdict: Verdict
    reasoning: str
    risks: list[str] = Field(default_factory=list)
    model_used: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    # v1.3 aditif: hasil gabungan quant + AI (AI tidak bisa menaikkan status)
    final_decision: Verdict | None = None
    final_reasons: list[str] = Field(default_factory=list)


class AiAnalyzeRequest(BaseModel):
    signal_id: str


class PairInfo(BaseModel):
    symbol: str              # simbol internal SniperRex
    td_symbol: str           # simbol yang diterima Twelve Data (mis. XAU/USD)
    name: str
    category: str            # Forex | Metal | Crypto
    pip_size: float          # XAUUSD pip = 0.1 (v1.1 gap #8)
    point_size: float        # XAUUSD point = 0.01
    digits: int


class TierInfo(BaseModel):
    tier: str
    timeframes: list[str]
    htf_proxy: str


class HealthInfo(BaseModel):
    status: str
    version: str
    uptime_sec: float


class ProviderHealth(BaseModel):
    ok: bool
    detail: str
    extra: dict[str, Any] = Field(default_factory=dict)

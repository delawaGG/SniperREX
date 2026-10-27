"""SniperRex backend configuration.

Semua magic number WAJIB di sini (spec §1 prinsip 4: No Magic Numbers).
"""
from __future__ import annotations

from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict

DISCLAIMER = (
    "Signal only. Not financial advice. Trade at your own risk. "
    "SniperRex tidak pernah mengeksekusi order."
)


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "SniperRex"
    version: str = "0.1.0"

    # --- Twelve Data ---
    twelve_data_api_key: str = ""
    twelve_base_url: str = "https://api.twelvedata.com"
    twelve_timeout_sec: float = 15.0
    twelve_max_retries: int = 2
    twelve_max_credits_per_min: int = 8   # FREE tier (spec §B15)

    # TTL cache per timeframe (detik). FREE tier = 8 kredit/menit (spec §B15).
    cache_ttl_sec: dict[str, int] = {
        "M15": 60, "M30": 120, "M45": 180, "H1": 300, "H4": 900, "D1": 3600,
    }

    # --- Timeframe policy (keputusan v1.1) ---
    # FREE: HTF alignment pakai M30 sebagai proxy (M30 - M45 tidak tersedia di vendor).
    free_timeframes: tuple[str, ...] = ("M15", "M30", "M45")
    pro_timeframes: tuple[str, ...] = ("M15", "M30", "M45", "H1", "H4", "D1")
    free_htf_proxy: str = "M45"   # v1.2: M45 tersedia di vendor -> proxy HTF FREE
    pro_htf_timeframes: tuple[str, ...] = ("H1", "H4")

    # --- Confluence gate (keputusan v1.1: fixed DAN proporsional) ---
    # 4 filter dipilih -> fixed 3/4. <4 filter -> ceil(ratio * n), minimal 2.
    confluence_threshold_fixed: int = 3
    confluence_ratio: float = 0.66
    confluence_min_threshold: int = 2

    # --- Risk / levels ---
    sl_buffer_atr: float = 0.2         # satu-satunya sumber buffer SL (v1.1: SL tunggal)
    tp_rr_multiples: tuple[float, ...] = (2.0, 3.0, 4.0)  # TP1=2R, TP2=3R, TP3=4R

    # --- Scoring (spec §10) ---
    score_weight_core: int = 30
    score_weight_confluence: int = 25
    score_weight_rr: int = 20
    score_weight_trend: int = 15
    score_weight_volatility: int = 10
    atr_pct_sweet_low: float = 0.0005   # 0.05%
    atr_pct_sweet_high: float = 0.003   # 0.30%
    score_high_confidence: int = 70

    # --- Volume / RSI / ATR filter defaults (spec §8) ---
    rsi_period: int = 14
    rsi_overbought: float = 70.0
    rsi_oversold: float = 30.0
    volume_ma_period: int = 20
    volume_mult: float = 1.2
    ob_proximity_atr: float = 1.0

    # --- Signal store ---
    signal_ttl_sec: int = 86_400     # in-memory (keputusan v1.1); hilang saat restart
    signal_settle_grace_sec: int = 90  # cegah duplikat pada bar yang sama

    # --- AI (Sprint 3) ---
    ai_provider: str = "groq"
    ai_base_url: str = "https://api.groq.com/openai/v1"
    ai_api_key: str = ""
    ai_model: str = "llama-3.3-70b-versatile"
    ai_timeout_sec: float = 30.0
    ai_max_retries: int = 2

    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"


@lru_cache
def get_settings() -> Settings:
    return Settings()

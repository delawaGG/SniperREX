"""HTTP client Twelve Data — satu-satunya jalur HTTP keluar untuk market data.

Dua pagar penting di FREE tier (8 kredit/menit):
  1. cache TTL per timeframe
  2. rate limiter lokal (menunggu, bukan menabrak 429)
Error tidak pernah memuat API key atau URL berkunci.
"""
from __future__ import annotations

import asyncio
import logging
import time
from collections import deque
from typing import Any

import httpx
import pandas as pd

from ..config import Settings

log = logging.getLogger(__name__)

# Internal SniperRex -> interval Twelve Data (vendor memakai "15min", "1h", dst).
VENDOR_INTERVAL = {
    "M15": "15min",
    "M30": "30min",
    "M45": "45min",     # ternyata TERSEDIA di vendor (koreksi v1.2)
    "H1": "1h",
    "H4": "4h",
    "D1": "1day",
}
VALID_INTERVALS = set(VENDOR_INTERVAL)


class TwelveDataError(RuntimeError):
    def __init__(self, message: str, code: int | str | None = None) -> None:
        super().__init__(message)
        self.code = code


class TwelveDataClient:
    def __init__(self, settings: Settings, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self._s = settings
        self._transport = transport
        self._cache: dict[str, tuple[float, pd.DataFrame]] = {}
        self._lock = asyncio.Lock()
        self._calls: deque[float] = deque()

    # ---------------- internal ----------------
    def _sanitize(self, text: str) -> str:
        key = self._s.twelve_data_api_key
        return text.replace(key, "***") if key else text

    async def _throttle(self) -> None:
        """Token bucket sederhana: maksimal N kredit per 60 detik."""
        limit = max(1, self._s.twelve_max_credits_per_min)
        while True:
            now = time.time()
            while self._calls and now - self._calls[0] > 60:
                self._calls.popleft()
            if len(self._calls) < limit:
                self._calls.append(now)
                return
            wait = 60 - (now - self._calls[0]) + 0.2
            log.info("rate limit lokal: tunggu %.1f detik", wait)
            await asyncio.sleep(wait)

    async def _get(self, path: str, params: dict[str, Any]) -> dict[str, Any]:
        if not self._s.twelve_data_api_key:
            raise TwelveDataError("TWELVE_DATA_API_KEY belum diset", "no_key")

        params = {**params, "apikey": self._s.twelve_data_api_key}
        url = f"{self._s.twelve_base_url.rstrip('/')}/{path.lstrip('/')}"
        last: Exception | None = None

        for attempt in range(self._s.twelve_max_retries + 1):
            await self._throttle()
            try:
                async with httpx.AsyncClient(timeout=self._s.twelve_timeout_sec,
                                             transport=self._transport) as client:
                    r = await client.get(url, params=params)
            except Exception as exc:  # noqa: BLE001  (network layer)
                last = exc
                await asyncio.sleep(2 ** attempt)
                continue

            try:
                body = r.json()
            except ValueError:
                body = None

            if r.status_code == 429 or (isinstance(body, dict) and body.get("code") == 429):
                raise TwelveDataError(
                    "rate limit Twelve Data (FREE = 8 kredit/menit)", 429)

            if body is None:
                raise TwelveDataError(
                    self._sanitize(f"HTTP {r.status_code} dari Twelve Data"), r.status_code)

            if r.status_code >= 400 or body.get("status") == "error":
                message = body.get("message") or f"HTTP {r.status_code}"
                raise TwelveDataError(self._sanitize(str(message)), body.get("code", r.status_code))

            return body

        raise TwelveDataError(self._sanitize(f"gagal menghubungi Twelve Data: {last}"), "http")

    # ---------------- public ----------------
    async def detect_tier(self) -> dict[str, Any]:
        data = await self._get("api_usage", {})
        plan = str(data.get("plan_category") or data.get("plan") or "").lower()
        tier = "PRO" if any(k in plan for k in ("pro", "grow", "enterprise", "business")) else "FREE"
        return {"tier": tier, "plan_raw": plan or "unknown", "usage": data}

    async def fetch_ohlcv(self, symbol: str, interval: str, outputsize: int = 200) -> pd.DataFrame:
        """`symbol` harus format Twelve Data (mis. XAU/USD)."""
        interval = interval.upper()
        if interval not in VALID_INTERVALS:
            raise TwelveDataError(f"interval tidak didukung vendor: {interval}", "bad_interval")

        key = f"{symbol.upper()}:{interval}:{outputsize}"
        ttl = self._s.cache_ttl_sec.get(interval, 60)
        now = time.time()
        async with self._lock:
            hit = self._cache.get(key)
            if hit and now - hit[0] < ttl:
                return hit[1].copy()

        data = await self._get("time_series", {
            "symbol": symbol.upper(),
            "interval": VENDOR_INTERVAL[interval],
            "outputsize": outputsize,
            "order": "ASC",
            "timezone": "UTC",
        })
        values = data.get("values") or data.get("data") or []
        if not values:
            raise TwelveDataError(f"tidak ada data OHLCV untuk {symbol} {interval}", "empty")

        df = pd.DataFrame(values)
        for col in ("open", "high", "low", "close", "volume"):
            df[col] = pd.to_numeric(df.get(col, 0), errors="coerce")
        df["datetime"] = pd.to_datetime(df["datetime"], utc=True)
        df = df.set_index("datetime").sort_index()
        df["volume"] = df["volume"].fillna(0)

        async with self._lock:
            self._cache[key] = (now, df)
        return df.copy()

    def clear_cache(self) -> None:
        self._cache.clear()

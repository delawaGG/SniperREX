"""In-memory signal store (keputusan v1.1) + dedupe + TTL.

Batasan yang disadari: data hilang saat proses restart. Migrasi ke SQLite
dijadwalkan sebelum MVP keluar dari dev (lihat README catatan teknis).
"""
from __future__ import annotations

import asyncio
import time

from ..models.signal import AiVerdict, Signal, SignalStatus


class SignalStore:
    def __init__(self, ttl_sec: int = 86_400, grace_sec: int = 90) -> None:
        self._ttl = ttl_sec
        self._grace = grace_sec
        self._items: dict[str, tuple[float, Signal]] = {}
        self._by_key: dict[str, str] = {}
        self._verdicts: dict[str, AiVerdict] = {}
        self._lock = asyncio.Lock()

    def _sweep(self, now: float) -> None:
        expired = [sid for sid, (ts, _) in self._items.items() if now - ts > self._ttl]
        for sid in expired:
            sig = self._items.pop(sid)[1]
            self._by_key.pop(sig.dedupe_key, None)

    async def add(self, signal: Signal) -> tuple[Signal, bool]:
        """Return (signal, created). created=False bila duplikat dalam grace window."""
        async with self._lock:
            now = time.time()
            self._sweep(now)
            existing_id = self._by_key.get(signal.dedupe_key)
            if existing_id and existing_id in self._items:
                ts, existing = self._items[existing_id]
                if now - ts < self._grace:
                    return existing, False
            self._items[signal.id] = (now, signal)
            self._by_key[signal.dedupe_key] = signal.id
            return signal, True

    async def get(self, signal_id: str) -> Signal | None:
        async with self._lock:
            self._sweep(time.time())
            item = self._items.get(signal_id)
            return item[1] if item else None

    async def list(self, pair: str | None = None, status: SignalStatus | None = None,
                   limit: int = 50) -> list[Signal]:
        async with self._lock:
            self._sweep(time.time())
            items = [sig for _, sig in self._items.values()]
        if pair:
            items = [s for s in items if s.pair.upper() == pair.upper()]
        if status:
            items = [s for s in items if s.status is status]
        items.sort(key=lambda s: s.created_at, reverse=True)
        return items[:limit]

    async def update_status(self, signal_id: str, status: SignalStatus) -> Signal | None:
        """Satu-satunya mutasi yang diizinkan pada signal (spec §14.4)."""
        async with self._lock:
            item = self._items.get(signal_id)
            if not item:
                return None
            updated = item[1].model_copy(update={"status": status})
            self._items[signal_id] = (item[0], updated)
            return updated

    async def set_verdict(self, verdict: AiVerdict) -> None:
        async with self._lock:
            self._verdicts[verdict.signal_id] = verdict

    async def get_verdict(self, signal_id: str) -> AiVerdict | None:
        async with self._lock:
            return self._verdicts.get(signal_id)

    async def clear(self) -> None:
        async with self._lock:
            self._items.clear()
            self._by_key.clear()
            self._verdicts.clear()

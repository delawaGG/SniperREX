from __future__ import annotations

from fastapi import APIRouter, HTTPException, Query

from ..config import DISCLAIMER, get_settings
from ..deps import get_store
from ..models.signal import (
    SignalEnvelope,
    SignalListEnvelope,
    SignalStatus,
    StatusUpdate,
)

router = APIRouter(tags=["signals"])


@router.get("/signals", response_model=SignalListEnvelope)
async def list_signals(
    pair: str | None = None,
    status: SignalStatus | None = None,
    limit: int = Query(default=50, ge=1, le=200),
) -> SignalListEnvelope:
    items = await get_store().list(pair=pair, status=status, limit=limit)
    return SignalListEnvelope(disclaimer=DISCLAIMER, signals=items, count=len(items))


@router.get("/signals/{signal_id}", response_model=SignalEnvelope)
async def get_signal(signal_id: str) -> SignalEnvelope:
    sig = await get_store().get(signal_id)
    if not sig:
        raise HTTPException(status_code=404, detail="signal tidak ditemukan")
    return SignalEnvelope(disclaimer=DISCLAIMER, signal=sig)


@router.put("/signals/{signal_id}/status", response_model=SignalEnvelope)
async def update_status(signal_id: str, body: StatusUpdate) -> SignalEnvelope:
    sig = await get_store().update_status(signal_id, body.status)
    if not sig:
        raise HTTPException(status_code=404, detail="signal tidak ditemukan")
    return SignalEnvelope(disclaimer=DISCLAIMER, signal=sig)


@router.get("/signals-meta/config")
async def signals_config() -> dict:
    s = get_settings()
    return {"ttl_sec": s.signal_ttl_sec, "grace_sec": s.signal_settle_grace_sec,
            "storage": "in_memory"}

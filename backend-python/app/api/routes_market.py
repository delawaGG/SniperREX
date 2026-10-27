from __future__ import annotations

from fastapi import APIRouter, Header

from ..config import DISCLAIMER
from ..deps import KEY_HEADER, client_for, service_for
from ..models.signal import ScanRequest, SignalEnvelope, TierInfo
from ..symbols import PAIRS

router = APIRouter(tags=["market"])


@router.get("/tier-info", response_model=TierInfo)
async def tier_info(x_twelve_key: str | None = Header(default=None, alias=KEY_HEADER)) -> TierInfo:
    svc = service_for(x_twelve_key)
    tf = await svc.allowed_timeframes()
    cfg = svc._cfg  # noqa: SLF001 — config read-only
    is_pro = len(tf) > len(cfg.free_timeframes)
    return TierInfo(
        tier="PRO" if is_pro else "FREE",
        timeframes=tf,
        htf_proxy=cfg.pro_htf_timeframes[0] if is_pro else cfg.free_htf_proxy,
    )


@router.get("/pairs")
async def pairs() -> dict:
    return {"pairs": [p.model_dump() for p in PAIRS.values()]}


@router.post("/scan", response_model=SignalEnvelope)
async def scan(req: ScanRequest,
               x_twelve_key: str | None = Header(default=None, alias=KEY_HEADER)) -> SignalEnvelope:
    outcome = await service_for(x_twelve_key).run(req)
    return SignalEnvelope(
        disclaimer=DISCLAIMER,
        signal=outcome.signal,
        reason=outcome.reason,
        warnings=outcome.warnings,
    )


@router.get("/tier-check")
async def tier_check(x_twelve_key: str | None = Header(default=None, alias=KEY_HEADER)) -> dict:
    """Dipakai SettingsScreen Android untuk tombol "Test Connection"."""
    client = client_for(x_twelve_key)
    info = await client.detect_tier()
    return {"ok": True, "tier": info["tier"], "plan": info["plan_raw"],
            "limit_per_min": info["usage"].get("plan_limit")}

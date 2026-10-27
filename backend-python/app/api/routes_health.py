from __future__ import annotations

import time

from fastapi import APIRouter, Header, Request

from ..config import get_settings
from ..clients.twelve_data_client import TwelveDataError
from ..services.ai_client import AiError
from ..deps import ai_client_for, get_client
from ..models.signal import HealthInfo, ProviderHealth

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthInfo)
async def health(request: Request) -> HealthInfo:
    started = getattr(request.app.state, "started_at", time.time())
    s = get_settings()
    return HealthInfo(status="ok", version=s.version, uptime_sec=round(time.time() - started, 1))


@router.get("/health/twelve", response_model=ProviderHealth)
async def health_twelve() -> ProviderHealth:
    s = get_settings()
    if not s.twelve_data_api_key:
        return ProviderHealth(ok=False, detail="TWELVE_DATA_API_KEY belum diset")
    try:
        info = await get_client().detect_tier()
        return ProviderHealth(ok=True, detail=f"terhubung, tier {info['tier']}",
                              extra={"tier": info["tier"], "plan": info["plan_raw"]})
    except TwelveDataError as exc:
        return ProviderHealth(ok=False, detail=str(exc), extra={"code": str(exc.code)})


@router.get("/health/ai", response_model=ProviderHealth)
async def health_ai(x_ai_key: str | None = Header(default=None, alias="X-AI-Key"),
                    x_ai_base_url: str | None = Header(default=None, alias="X-AI-Base-Url"),
                    x_ai_model: str | None = Header(default=None, alias="X-AI-Model")) -> ProviderHealth:
    try:
        info = await ai_client_for(x_ai_key, x_ai_base_url, x_ai_model).ping()
    except AiError as exc:
        return ProviderHealth(ok=False, detail=str(exc), extra={"code": str(exc.code)})
    return ProviderHealth(ok=True, detail="endpoint AI terjangkau",
                          extra={"model": info["model_configured"],
                                 "model_count": len(info["models"])})

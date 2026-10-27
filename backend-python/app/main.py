"""SniperRex backend — signal only, tidak ada endpoint eksekusi order."""
from __future__ import annotations

import logging
import time

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from .api import routes_ai, routes_health, routes_market, routes_signal
from .clients.twelve_data_client import TwelveDataError
from .config import DISCLAIMER, get_settings
from .services.ai_client import AiError
from .services.scan_service import ScanError

logging.basicConfig(level=get_settings().log_level,
                    format="%(asctime)s %(levelname)s %(name)s %(message)s")

BLOCKED_PATHS = ("/order", "/execute", "/buy", "/sell", "/positions")


def create_app() -> FastAPI:
    s = get_settings()
    app = FastAPI(
        title=f"{s.app_name} API",
        version=s.version,
        description="Trading signal API. Signal only — no auto-execution. " + DISCLAIMER,
    )
    app.state.started_at = time.time()

    for module in (routes_health, routes_market, routes_signal, routes_ai):
        app.include_router(module.router, prefix="/api")

    @app.middleware("http")
    async def block_execution(request: Request, call_next):
        """Guard keras: endpoint eksekusi tidak boleh pernah ada (spec §14.1)."""
        if any(request.url.path.endswith(p) for p in BLOCKED_PATHS):
            return JSONResponse(
                status_code=404,
                content={"detail": "SniperRex adalah signal-only. Endpoint eksekusi tidak ada."},
            )
        response = await call_next(request)
        if request.url.path.startswith("/api/signal"):
            response.headers["X-Disclaimer"] = "signal-only-not-financial-advice"
        return response

    @app.exception_handler(ScanError)
    async def scan_error_handler(_: Request, exc: ScanError):
        return JSONResponse(status_code=exc.status_code,
                            content={"disclaimer": DISCLAIMER, "signal": None,
                                     "reason": str(exc), "warnings": exc.warnings})

    @app.exception_handler(TwelveDataError)
    async def twelve_error_handler(_: Request, exc: TwelveDataError):
        if exc.code == "no_key":
            code = 503
        elif exc.code == 429:
            code = 429
        else:
            code = 502
        return JSONResponse(status_code=code,
                            content={"disclaimer": DISCLAIMER, "signal": None,
                                     "reason": str(exc), "warnings": []})

    @app.exception_handler(AiError)
    async def ai_error_handler(_: Request, exc: AiError):
        if exc.code == "no_key":
            code = 503
        elif exc.code == 429:
            code = 429
        else:
            code = 502
        return JSONResponse(status_code=code,
                            content={"disclaimer": DISCLAIMER, "signal": None,
                                     "reason": str(exc), "warnings": []})

    @app.get("/", include_in_schema=False)
    async def root() -> dict:
        return {"app": s.app_name, "version": s.version, "docs": "/docs",
                "disclaimer": DISCLAIMER}

    return app


app = create_app()

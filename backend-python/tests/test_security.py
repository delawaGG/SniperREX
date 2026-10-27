"""Pagar keamanan: API key tidak boleh muncul di pesan error."""
from __future__ import annotations

import asyncio

import httpx
import pytest

from app.clients.twelve_data_client import TwelveDataClient, TwelveDataError
from app.config import Settings

SECRET = "test-only-fake-key-0123456789abcdef"   # key palsu, bukan kredensial nyata


def _client(handler) -> TwelveDataClient:
    cfg = Settings(twelve_data_api_key=SECRET)
    return TwelveDataClient(cfg, transport=httpx.MockTransport(handler))


def test_error_message_never_contains_key():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(400, json={
            "code": 400, "status": "error",
            "message": f"bad request, apikey={SECRET} expired",
        })

    client = _client(handler)
    with pytest.raises(TwelveDataError) as exc:
        asyncio.run(client.fetch_ohlcv("XAU/USD", "M15", 100))
    message = str(exc.value)
    assert SECRET not in message
    assert "***" in message


def test_http_error_without_json_body_is_sanitized():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, text="<html>proxy error</html>")

    client = _client(handler)
    with pytest.raises(TwelveDataError) as exc:
        asyncio.run(client.detect_tier())
    assert SECRET not in str(exc.value)


def test_missing_key_short_circuits():
    cfg = Settings(twelve_data_api_key="")
    client = TwelveDataClient(cfg, transport=httpx.MockTransport(
        lambda request: httpx.Response(200, json={"plan": "basic"})))
    with pytest.raises(TwelveDataError) as exc:
        asyncio.run(client.detect_tier())
    assert exc.value.code == "no_key"

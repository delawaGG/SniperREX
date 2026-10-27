from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.config import DISCLAIMER, get_settings
from app.main import app


@pytest.fixture(scope="session")
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture(autouse=True)
def _clean_settings_cache():
    get_settings.cache_clear()
    yield
    get_settings.cache_clear()


def test_health_ok(client: TestClient):
    r = client.get("/api/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert "uptime_sec" in body


def test_pairs_catalog_has_pip_and_point(client: TestClient):
    r = client.get("/api/pairs")
    assert r.status_code == 200
    pairs = {p["symbol"]: p for p in r.json()["pairs"]}
    assert pairs["XAUUSD"]["pip_size"] == 0.1     # v1.1: pip != point
    assert pairs["XAUUSD"]["point_size"] == 0.01
    assert pairs["USDJPY"]["pip_size"] == 0.01


def test_execution_endpoints_do_not_exist(client: TestClient):
    for path in ("/api/order", "/api/execute", "/api/buy", "/api/sell"):
        assert client.post(path, json={}).status_code == 404


def test_scan_without_api_key_returns_503(client: TestClient):
    r = client.post("/api/scan", json={
        "pair": "XAUUSD", "timeframe": "M15", "core_strategy": "FVG",
        "confirmations": ["RSI", "ATR", "OB"],
    })
    assert r.status_code == 503
    assert r.json()["signal"] is None
    assert r.json()["disclaimer"] == DISCLAIMER


def test_scan_unknown_pair_rejected(client: TestClient):
    r = client.post("/api/scan", json={
        "pair": "ZZZUSD", "timeframe": "M15", "core_strategy": "FVG",
        "confirmations": ["RSI"],
    })
    # validasi pair terjadi setelah cek tier -> tanpa key = 503; dengan key = 400.
    assert r.status_code in (400, 503)


def test_ai_analyze_unknown_signal(client: TestClient):
    r = client.post("/api/ai/analyze", json={"signal_id": "abc"})
    assert r.status_code == 404


def test_signals_empty(client: TestClient):
    r = client.get("/api/signals")
    assert r.status_code == 200
    body = r.json()
    assert body["count"] == 0
    assert body["disclaimer"] == DISCLAIMER

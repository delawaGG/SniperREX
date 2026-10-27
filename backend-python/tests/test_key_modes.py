"""Mode key: server (.env) vs per-request header — tanpa pernah menyimpan key user."""
from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app import deps
from app.clients.twelve_data_client import TwelveDataClient
from app.main import app

USER_KEY = "user-supplied-key-123"


@pytest.fixture()
def client() -> TestClient:
    deps._client_cache.clear()
    return TestClient(app)


def test_header_key_builds_isolated_client(client: TestClient):
    c1 = deps.client_for(USER_KEY)
    c2 = deps.client_for("other-key")
    assert isinstance(c1, TwelveDataClient)
    assert c1 is not c2
    assert c1 is deps.client_for(USER_KEY)         # di-cache per key
    assert c1._s.twelve_data_api_key == USER_KEY   # noqa: SLF001


def test_no_header_falls_back_to_server_client(client: TestClient):
    assert deps.client_for(None) is deps.get_client()
    assert deps.client_for("") is deps.get_client()


def test_pairs_endpoint_never_needs_key(client: TestClient):
    r = client.get("/api/pairs")
    assert r.status_code == 200
    for p in r.json()["pairs"]:
        assert "td_symbol" in p and "/" in p["td_symbol"]


def test_client_cache_is_bounded(client: TestClient):
    for i in range(deps._MAX_CLIENT_CACHE + 5):
        deps.client_for(f"key-{i}")
    assert len(deps._client_cache) <= deps._MAX_CLIENT_CACHE


def test_tier_check_without_key_is_503(client: TestClient):
    r = client.get("/api/tier-check")
    assert r.status_code == 503
    assert "belum diset" in r.json()["reason"]

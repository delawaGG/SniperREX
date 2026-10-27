"""Dependency singletons (Sprint 1 — in-memory).

Dua mode API key (untuk aplikasi yang dibagikan ke teman):
  1. Server-side: TWELVE_DATA_API_KEY di .env (dev / instance pribadi)
  2. Per-request: header `X-Twelve-Key` — key milik user, tidak disimpan server
"""
from __future__ import annotations

from functools import lru_cache

from .clients.twelve_data_client import TwelveDataClient
from .services.ai_client import AiClient
from .config import get_settings
from .services.scan_service import ScanService
from .store.memory_store import SignalStore

KEY_HEADER = "X-Twelve-Key"
_MAX_CLIENT_CACHE = 32


@lru_cache
def get_client() -> TwelveDataClient:
    return TwelveDataClient(get_settings())


@lru_cache
def get_store() -> SignalStore:
    s = get_settings()
    return SignalStore(ttl_sec=s.signal_ttl_sec, grace_sec=s.signal_settle_grace_sec)


@lru_cache
def get_scan_service() -> ScanService:
    return ScanService(get_client(), get_store(), get_settings())


_client_cache: dict[str, TwelveDataClient] = {}


def client_for(user_key: str | None) -> TwelveDataClient:
    """Client dengan key user (header) atau key server (.env)."""
    if not user_key:
        return get_client()
    key = user_key.strip()
    if not key:
        return get_client()
    client = _client_cache.get(key)
    if client is None:
        cfg = get_settings().model_copy(update={"twelve_data_api_key": key})
        client = TwelveDataClient(cfg)
        if len(_client_cache) >= _MAX_CLIENT_CACHE:
            _client_cache.clear()      # buang cache lama, hindari memory leak
        _client_cache[key] = client
    return client


_ai_cache: dict[tuple[str, str, str], AiClient] = {}


def ai_client_for(user_key: str | None = None, base_url: str | None = None,
                  model: str | None = None) -> AiClient:
    """AI client: key/endpoint/model dari header user, fallback ke .env."""
    s = get_settings()
    key = (user_key or "").strip() or s.ai_api_key
    base = (base_url or "").strip() or s.ai_base_url
    mdl = (model or "").strip() or s.ai_model
    cache_key = (key, base, mdl)
    client = _ai_cache.get(cache_key)
    if client is None:
        cfg = s.model_copy(update={"ai_api_key": key, "ai_base_url": base, "ai_model": mdl})
        client = AiClient(cfg)
        if len(_ai_cache) >= _MAX_CLIENT_CACHE:
            _ai_cache.clear()
        _ai_cache[cache_key] = client
    return client


def service_for(user_key: str | None) -> ScanService:
    client = client_for(user_key)
    if client is get_client():
        return get_scan_service()
    return ScanService(client, get_store(), get_settings())

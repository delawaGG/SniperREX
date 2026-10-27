"""Test selalu offline: paksa key kosong walau .env lokal terisi.

Precedence pydantic-settings: environment variable > file .env.
"""
from __future__ import annotations

import os

os.environ["TWELVE_DATA_API_KEY"] = ""
os.environ["AI_API_KEY"] = ""


import pytest


@pytest.fixture(autouse=True)
def _clean_signal_store():
    """Store in-memory bersifat global -> wajib diisolasi antar test."""
    import asyncio

    from app.deps import get_store

    asyncio.run(get_store().clear())
    yield
    asyncio.run(get_store().clear())

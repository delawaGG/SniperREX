"""Routes AI: second opinion + chat kontekstual signal."""
from __future__ import annotations

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field

from ..deps import ai_client_for, get_store
from ..models.signal import AiAnalyzeRequest, AiVerdict
from ..services.decision import final_decision

router = APIRouter(prefix="/ai", tags=["ai"])


class ChatRequest(BaseModel):
    signal_id: str
    message: str = Field(min_length=1, max_length=2000)
    history: list[dict] = Field(default_factory=list)


class ChatReply(BaseModel):
    signal_id: str
    reply: str


@router.post("/analyze", response_model=AiVerdict)
async def analyze(
    body: AiAnalyzeRequest,
    x_ai_key: str | None = Header(default=None, alias="X-AI-Key"),
    x_ai_base_url: str | None = Header(default=None, alias="X-AI-Base-Url"),
    x_ai_model: str | None = Header(default=None, alias="X-AI-Model"),
) -> AiVerdict:
    store = get_store()
    signal = await store.get(body.signal_id)
    if signal is None:
        raise HTTPException(status_code=404, detail="signal tidak ditemukan")

    client = ai_client_for(x_ai_key, x_ai_base_url, x_ai_model)
    verdict = await client.analyze(signal)

    decision = final_decision(signal.quant_score, verdict)
    verdict = verdict.model_copy(update={
        "final_decision": decision.verdict,
        "final_reasons": decision.reasons,
    })
    await store.set_verdict(verdict)
    return verdict


@router.post("/chat", response_model=ChatReply)
async def chat(
    body: ChatRequest,
    x_ai_key: str | None = Header(default=None, alias="X-AI-Key"),
    x_ai_base_url: str | None = Header(default=None, alias="X-AI-Base-Url"),
    x_ai_model: str | None = Header(default=None, alias="X-AI-Model"),
) -> ChatReply:
    signal = await get_store().get(body.signal_id)
    if signal is None:
        raise HTTPException(status_code=404, detail="signal tidak ditemukan")
    client = ai_client_for(x_ai_key, x_ai_base_url, x_ai_model)
    reply = await client.chat(signal, body.message, body.history)
    return ChatReply(signal_id=signal.id, reply=reply)


@router.get("/verdict/{signal_id}", response_model=AiVerdict)
async def get_verdict(signal_id: str) -> AiVerdict:
    verdict = await get_store().get_verdict(signal_id)
    if verdict is None:
        raise HTTPException(status_code=404, detail="belum ada verdict untuk signal ini")
    return verdict

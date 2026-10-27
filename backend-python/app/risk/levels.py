"""Risk calculator — SATU-SATUNYA otoritas SL/TP (v1.1: SL buffer tunggal)."""
from __future__ import annotations

from dataclasses import dataclass

from ..config import Settings
from ..models.signal import Direction


@dataclass(frozen=True)
class Levels:
    entry: float
    stop_loss: float
    take_profit: tuple[float, ...]
    risk: float            # jarak entry -> SL (harga absolut)
    rr: float              # RR TP1 (basis yang dipakai di field risk_reward)
    rr_extension: float    # RR TP3 — dipakai komponen skor (lihat catatan)
    risk_pips: float


def _r(value: float, point_size: float) -> float:
    digits = max(0, len(f"{point_size:.10f}".rstrip("0").split(".")[-1]))
    return round(value, digits)


def calculate_levels(
    *,
    direction: Direction,
    entry: float,
    structure_level: float,
    atr: float,
    point_size: float,
    pip_size: float,
    cfg: Settings,
) -> Levels:
    """SL = structure_level ∓ ATR*buffer, TP = entry ± risk*[2,3,4].

    Catatan v1.1 (keputusan Anto: RR basis TP1):
      - `risk_reward` = RR TP1 -> selalu 2.00 karena konstruksi TP1 = 2R.
      - Komponen skor RR memakai `rr_extension` (TP3 = 4R), kalau tidak
        bobot 20 poin di spec §10 akan jadi nilai konstan (dead weight).
    """
    if atr <= 0:
        raise ValueError("ATR harus > 0")
    if entry <= 0:
        raise ValueError("entry harus > 0")

    sign = 1.0 if direction is Direction.LONG else -1.0
    sl_raw = structure_level - sign * atr * cfg.sl_buffer_atr
    risk = abs(entry - sl_raw)
    if risk <= 0:
        raise ValueError("SL terlalu dekat dengan entry (risk = 0)")

    tps = tuple(_r(entry + sign * risk * m, point_size) for m in cfg.tp_rr_multiples)
    rr_tp1 = abs(tps[0] - entry) / risk
    rr_tp3 = abs(tps[-1] - entry) / risk

    return Levels(
        entry=_r(entry, point_size),
        stop_loss=_r(sl_raw, point_size),
        take_profit=tps,
        risk=risk,
        rr=round(rr_tp1, 2),
        rr_extension=round(rr_tp3, 2),
        risk_pips=round(risk / pip_size, 1),
    )

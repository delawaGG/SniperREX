package com.sniperrex.app.domain.engine

import com.sniperrex.app.domain.model.Direction

/** Satu candle OHLCV tertutup. */
data class Candle(
    val timeMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
)

enum class StructureEvent { BOS, CHOCH, NONE }

/** Draft setup dari strategi (belum ada level & skor). */
data class Draft(
    val direction: Direction,
    val entry: Double,
    val structureLevel: Double,
    val coreStrength: Double,
    val event: StructureEvent = StructureEvent.NONE,
    val obZone: Pair<Double, Double>? = null,
    val note: String = "",
)

data class FilterCheck(
    val name: String,
    val passed: Boolean,
    val value: Double,
    val threshold: Double,
    val detail: String,
)

data class Levels(
    val entry: Double,
    val stopLoss: Double,
    val takeProfits: List<Double>,
    val risk: Double,
    val riskReward: Double,
    val riskRewardExtension: Double,
    val riskPips: Double,
)

data class ScoreBreakdown(val core: Int, val confluence: Int, val rr: Int, val trend: Int, val volatility: Int, val total: Int)

object Intervals {
    /** internal -> interval Twelve Data */
    val MAP = mapOf(
        "M15" to "15min",
        "M30" to "30min",
        "M45" to "45min",
        "H1" to "1h",
        "H4" to "4h",
        "D1" to "1day",
    )
}

package com.sniperrex.app.domain.model

/** Model domain read-only: hanya `status` yang boleh berubah. */
data class Signal(
    val id: String,
    val pair: String,
    val timeframe: String,
    val direction: Direction,
    val coreStrategy: CoreStrategy,
    val structureEvent: String,
    val confirmations: List<String>,
    val entry: Double,
    val stopLoss: Double,
    val takeProfits: List<Double>,
    val riskReward: Double,
    val quantScore: Int,
    val status: SignalStatus,
    val totalConfluence: Int,
    val threshold: Int,
    val trend: String,
    val rsi: Double,
    val atr: Double,
    val session: String,
    val createdAtEpochMs: Long,
    val aiVerdict: AiVerdict? = null,
) {
    val riskPerUnit: Double get() = kotlin.math.abs(entry - stopLoss)
}

enum class Direction { LONG, SHORT }

enum class CoreStrategy(val label: String) {
    FVG("Fair Value Gap"),
    ORDER_BLOCK_BOS("Order Block + BOS"),
    EMA_CROSS("EMA Cross"),
    TREND_STRUCTURE("Trend Structure"),
    MACD_CROSS("MACD Cross"),
}

enum class SignalStatus { WAIT, ACTIVE, HIT_TP, HIT_SL, SKIPPED }

enum class Verdict { TAKE, WAIT, SKIP }

data class AiVerdict(
    val aiScore: Int,
    val verdict: Verdict,
    val reasoning: String,
    val risks: List<String>,
    val finalDecision: Verdict? = null,
    val finalReasons: List<String> = emptyList(),
)

data class ScanParams(
    val pair: String,
    val timeframe: String,
    val coreStrategy: CoreStrategy,
    val confirmations: List<String>,
)

data class ScanResult(
    val signal: Signal?,
    val reason: String?,
    val warnings: List<String>,
)

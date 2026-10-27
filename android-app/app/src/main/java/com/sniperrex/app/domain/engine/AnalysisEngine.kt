package com.sniperrex.app.domain.engine

import com.sniperrex.app.domain.model.AiVerdict
import com.sniperrex.app.domain.model.CoreStrategy
import com.sniperrex.app.domain.model.PairInfo
import com.sniperrex.app.domain.model.ScanResult
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.SignalStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orkestrasi: fetch (di luar) -> strategi -> filter -> gate -> level -> skor -> Signal.
 * Pure: tidak menyentuh HTTP/DB, mudah diuji.
 */
@Singleton
class AnalysisEngine @Inject constructor() {

    fun analyse(
        info: PairInfo,
        timeframe: String,
        strategy: CoreStrategy,
        confirmations: List<String>,
        candles: List<Candle>,
        htfCandles: List<Candle>?,
        htfTimeframe: String = "M45",
        nowMs: Long = System.currentTimeMillis(),
    ): ScanResult {
        if (candles.size < 60) return ScanResult(null, "data ${info.symbol} $timeframe < 60 candle", emptyList())

        val atr = Indicators.atr(candles).lastOrNull() ?: 0.0
        val closes = DoubleArray(candles.size) { candles[it].close }
        val rsiValue = Indicators.rsi(closes).lastOrNull() ?: 50.0
        val price = candles.last().close

        val draft = Strategies.run(strategy, candles)
            ?: return ScanResult(
                null,
                "strategi ${strategy.name} tidak menemukan setup di bar tertutup terakhir",
                listOf("HTF $htfTimeframe dipakai untuk alignment"),
            )

        val alignment = Scoring.trendAlignment(draft.direction, htfCandles)

        val checks = Filters.run(confirmations, candles, info, draft.direction, draft.entry, draft.obZone)
        val passed = checks.count { it.passed }
        val threshold = Scoring.requiredThreshold(confirmations.size)
        val warnings = checks.map { "${it.name}:${if (it.passed) "PASS" else "FAIL"} ${it.detail}" }.toMutableList()

        if (passed < threshold) {
            return ScanResult(null, "konfluensi $passed/${confirmations.size} < gate $threshold", warnings)
        }

        val levels = Risk.calculate(
            direction = draft.direction,
            entry = draft.entry,
            structureLevel = draft.structureLevel,
            atr = atr,
            pointSize = info.pointSize,
            pipSize = info.pipSize,
        ) ?: return ScanResult(null, "level tidak valid (risk <= 0)", warnings)

        val atrPct = if (price > 0) atr / price else 0.0
        val score = Scoring.score(
            coreStrength = draft.coreStrength,
            passed = passed,
            selected = confirmations.size,
            rrExtension = levels.riskRewardExtension,
            trendAlignment = alignment,
            atrPct = atrPct,
        )

        val trend = trendOf(closes)
        val signal = Signal(
            id = UUID.randomUUID().toString(),
            pair = info.symbol,
            timeframe = timeframe,
            direction = draft.direction,
            coreStrategy = strategy,
            structureEvent = draft.event.name,
            confirmations = checks.filter { it.passed }.map { it.name },
            entry = levels.entry,
            stopLoss = levels.stopLoss,
            takeProfits = levels.takeProfits,
            riskReward = levels.riskReward,
            quantScore = score.total,
            status = SignalStatus.WAIT,
            totalConfluence = passed,
            threshold = threshold,
            trend = trend,
            rsi = rsiValue,
            atr = atr,
            session = Scoring.sessionOf(nowMs),
            createdAtEpochMs = nowMs,
            aiVerdict = null,
        )

        if (alignment == "counter") warnings.plusAssign("HTF $htfTimeframe melawan arah signal")
        if (score.total < 50) warnings.plusAssign("quant_score rendah (${score.total}) — pertimbangkan SKIP")
        return ScanResult(signal, null, warnings)
    }

    fun applyVerdict(signal: Signal, ai: AiVerdict): AiVerdict {
        val final = Scoring.finalDecision(signal.quantScore, ai.verdict.name, ai.aiScore)
        val reasons = mutableListOf("quant_score ${signal.quantScore}", "AI ${ai.verdict.name} (${ai.aiScore})")
        return ai.copy(finalDecision = com.sniperrex.app.domain.model.Verdict.valueOf(final), finalReasons = reasons)
    }

    private fun trendOf(closes: DoubleArray): String {
        if (closes.size < 60) return "-"
        val fast = Indicators.ema(closes, 21).last()
        val slow = Indicators.ema(closes, 55).last()
        return if (fast > slow) "BULLISH" else "BEARISH"
    }
}

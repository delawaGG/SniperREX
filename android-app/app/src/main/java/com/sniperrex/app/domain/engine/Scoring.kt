package com.sniperrex.app.domain.engine

import kotlin.math.ceil

/**
 * Skor kuantitatif deterministik 0-100 (spec §10).
 * Catatan v1.1: komponen RR memakai perluasan TP3, karena RR TP1 selalu 2.00.
 */
object Scoring {

    private const val W_CORE = 30
    private const val W_CONFLUENCE = 25
    private const val W_RR = 20
    private const val W_TREND = 15
    private const val W_VOL = 10

    private const val ATR_SWEET_LOW = 0.0005
    private const val ATR_SWEET_HIGH = 0.0030

    /** Gate hybrid: 4 filter -> 3; <4 -> ceil(0.66n), minimal 2. */
    fun requiredThreshold(selected: Int): Int =
        if (selected >= 4) 3 else maxOf(2, minOf(selected, ceil(0.66 * selected).toInt()))

    fun score(
        coreStrength: Double,
        passed: Int,
        selected: Int,
        rrExtension: Double,
        trendAlignment: String,
        atrPct: Double,
    ): ScoreBreakdown {
        val core = (W_CORE * coreStrength.coerceIn(0.0, 1.0)).toInt()

        val ratio = if (selected > 0) passed.toDouble() / selected else 0.0
        val confluence = when {
            ratio >= 1.0 -> W_CONFLUENCE
            ratio >= 0.75 -> (W_CONFLUENCE * 0.72).toInt()
            ratio >= 0.5 -> (W_CONFLUENCE * 0.40).toInt()
            else -> 0
        }

        val rr = when {
            rrExtension >= 3.0 -> W_RR
            rrExtension >= 2.5 -> (W_RR * 0.75).toInt()
            rrExtension >= 2.0 -> (W_RR * 0.5).toInt()
            else -> (W_RR * 0.25).toInt()
        }

        val trend = when (trendAlignment) {
            "aligned" -> W_TREND
            "mixed" -> (W_TREND * 0.47).toInt()
            else -> 0
        }

        val volatility = when {
            atrPct in ATR_SWEET_LOW..ATR_SWEET_HIGH -> W_VOL
            atrPct in (ATR_SWEET_LOW * 0.5)..(ATR_SWEET_HIGH * 1.5) -> (W_VOL * 0.5).toInt()
            else -> 0
        }

        val total = (core + confluence + rr + trend + volatility).coerceIn(0, 100)
        return ScoreBreakdown(core, confluence, rr, trend, volatility, total)
    }

    fun trendAlignment(direction: com.sniperrex.app.domain.model.Direction, htfCandles: List<Candle>?): String {
        if (htfCandles == null || htfCandles.size < 60) return "mixed"
        val closes = DoubleArray(htfCandles.size) { htfCandles[it].close }
        val fast = Indicators.ema(closes, 21).last()
        val slow = Indicators.ema(closes, 55).last()
        val up = fast > slow
        return if ((direction == com.sniperrex.app.domain.model.Direction.LONG && up) ||
            (direction == com.sniperrex.app.domain.model.Direction.SHORT && !up)
        ) "aligned" else "counter"
    }

    fun sessionOf(epochMs: Long): String {
        val hour = ((epochMs / 3_600_000L) % 24L).toInt()
        return when {
            hour in 0..6 -> "ASIA"
            hour in 7..11 -> "LONDON"
            hour in 12..20 -> "NEW_YORK"
            else -> "SYDNEY"
        }
    }

    /** Matriks final v1.1: AI tidak bisa menaikkan status. */
    fun finalDecision(quantScore: Int, aiVerdict: String?, aiScore: Int?): String {
        if (quantScore < 50) return "SKIP"
        if (aiVerdict == null) return if (quantScore >= 70) "TAKE" else "WAIT"
        if (aiVerdict == "SKIP") return "SKIP"
        return if (quantScore >= 60 && (aiScore ?: 0) >= 60) "TAKE" else "WAIT"
    }
}

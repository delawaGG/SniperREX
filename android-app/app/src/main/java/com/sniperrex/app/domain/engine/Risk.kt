package com.sniperrex.app.domain.engine

import com.sniperrex.app.domain.model.Direction
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** Satu-satunya otoritas SL/TP. SL = struktur ∓ ATR*0.2. */
object Risk {

    private const val SL_BUFFER_ATR = 0.2
    private val TP_MULTIPLES = listOf(2.0, 3.0, 4.0)

    private fun roundTo(value: Double, pointSize: Double): Double {
        val step = pointSize
        if (step <= 0) return value
        return (value / step).roundToLong() * step
    }

    fun calculate(
        direction: Direction,
        entry: Double,
        structureLevel: Double,
        atr: Double,
        pointSize: Double,
        pipSize: Double,
    ): Levels? {
        if (atr <= 0 || entry <= 0) return null
        val sign = if (direction == Direction.LONG) 1.0 else -1.0
        val sl = structureLevel - sign * atr * SL_BUFFER_ATR
        val risk = abs(entry - sl)
        if (risk <= 0) return null
        val tps = TP_MULTIPLES.map { roundTo(entry + sign * risk * it, pointSize) }
        val rr = abs(tps.first() - entry) / risk
        val rrExt = abs(tps.last() - entry) / risk
        return Levels(
            entry = roundTo(entry, pointSize),
            stopLoss = roundTo(sl, pointSize),
            takeProfits = tps,
            risk = risk,
            riskReward = (rr * 100).roundToLong() / 100.0,
            riskRewardExtension = (rrExt * 100).roundToLong() / 100.0,
            riskPips = if (pipSize > 0) (risk / pipSize * 10).roundToLong() / 10.0 else 0.0,
        )
    }
}

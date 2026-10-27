package com.sniperrex.app.domain.engine

import com.sniperrex.app.domain.model.Direction

/** 5 strategi, dua arah. Semua hanya memakai candle tertutup. */
object Strategies {

    private const val FVG_LOOKBACK = 60
    private const val OB_SCAN = 80

    // ---------------- FVG ----------------
    fun fvg(candles: List<Candle>): Draft? {
        if (candles.size < FVG_LOOKBACK) return null
        val atr = Indicators.atr(candles).lastOrNull() ?: return null
        if (atr <= 0) return null
        val close = candles.last().close
        val start = candles.size - FVG_LOOKBACK
        val candidates = mutableListOf<Pair<Int, Pair<Direction, Pair<Double, Double>>>>()

        for (i in start + 2 until candles.size) {
            val lower = candles[i - 2].high
            val upper = candles[i].low
            if (upper > lower) {
                candidates.add(i to (Direction.LONG to (lower to upper)))
            } else {
                val bLower = candles[i].high
                val bUpper = candles[i - 2].low
                if (bLower < bUpper) candidates.add(i to (Direction.SHORT to (bLower to bUpper)))
            }
        }
        for ((idx, payload) in candidates.asReversed()) {
            val (direction, zone) = payload
            val lower = zone.first
            val upper = zone.second
            val after = candles.subList(idx + 1, candles.size)
            val mitigated = if (direction == Direction.LONG) after.any { it.close < lower }
                            else after.any { it.close > upper }
            if (mitigated) continue
            val mid = (lower + upper) / 2
            if (direction == Direction.LONG) {
                if (close < lower || close - mid > atr * 1.5) continue
            } else {
                if (close > upper || mid - close > atr * 1.5) continue
            }
            val structure = if (direction == Direction.LONG) lower else upper
            return Draft(
                direction = direction,
                entry = mid,
                structureLevel = structure,
                coreStrength = ((upper - lower) / atr).coerceIn(0.0, 1.0),
                obZone = lower to upper,
                note = "FVG $lower-$upper",
            )
        }
        return null
    }

    // ---------------- Order Block + BOS ----------------
    fun obBos(candles: List<Candle>): Draft? {
        if (candles.size < OB_SCAN / 2) return null
        val atr = Indicators.atr(candles).lastOrNull() ?: return null
        if (atr <= 0) return null
        val close = candles.last().close
        val impulse = atr * 1.5
        val windowStart = candles.size - minOf(candles.size, OB_SCAN)
        val highs = DoubleArray(candles.size) { candles[it].high }
        val lows = DoubleArray(candles.size) { candles[it].low }
        val (swingHighs, swingLows) = Indicators.swings(highs, lows)

        for (j in candles.size - 3 downTo maxOf(candles.size - 20, 1)) {
            val legEnd = minOf(j + 3, candles.size - 1)
            if (legEnd <= j) continue
            var legHigh = Double.NEGATIVE_INFINITY
            var legLow = Double.POSITIVE_INFINITY
            for (k in j + 1..legEnd) {
                if (candles[k].high > legHigh) legHigh = candles[k].high
                if (candles[k].low < legLow) legLow = candles[k].low
            }
            val bullish = legHigh - candles[j].low >= impulse
            val bearish = candles[j].high - legLow >= impulse
            if (!bullish && !bearish) continue

            val direction = if (bullish) Direction.LONG else Direction.SHORT
            var obIdx: Int? = null
            for (k in j downTo maxOf(j - 3, 0)) {
                val isDown = candles[k].close < candles[k].open
                val isUp = candles[k].close > candles[k].open
                if (direction == Direction.LONG && isDown) { obIdx = k; break }
                if (direction == Direction.SHORT && isUp) { obIdx = k; break }
            }
            val ob = obIdx ?: continue
            val obLow = candles[ob].low
            val obHigh = candles[ob].high
            val mid = (obLow + obHigh) / 2

            if (direction == Direction.LONG) {
                val bos = swingHighs.lastOrNull { it >= windowStart }?.let { highs[it] } ?: continue
                if (close <= obLow || close > bos + atr * 1.5) continue
                val event = if (close > bos) StructureEvent.BOS else StructureEvent.NONE
                return Draft(
                    direction = Direction.LONG,
                    entry = mid,
                    structureLevel = obLow,
                    coreStrength = ((legHigh - obLow) / (atr * 3)).coerceIn(0.0, 1.0),
                    event = event,
                    obZone = obLow to obHigh,
                    note = "OB $obLow-$obHigh",
                )
            } else {
                val bos = swingLows.lastOrNull { it >= windowStart }?.let { lows[it] } ?: continue
                if (close >= obHigh || close < bos - atr * 1.5) continue
                val event = if (close < bos) StructureEvent.BOS else StructureEvent.NONE
                return Draft(
                    direction = Direction.SHORT,
                    entry = mid,
                    structureLevel = obHigh,
                    coreStrength = ((obHigh - legLow) / (atr * 3)).coerceIn(0.0, 1.0),
                    event = event,
                    obZone = obLow to obHigh,
                    note = "OB $obLow-$obHigh",
                )
            }
        }
        return null
    }

    // ---------------- EMA Cross ----------------
    fun emaCross(candles: List<Candle>): Draft? {
        if (candles.size < 28) return null
        val closes = DoubleArray(candles.size) { candles[it].close }
        val fast = Indicators.ema(closes, 9)
        val slow = Indicators.ema(closes, 21)
        val atr = Indicators.atr(candles).lastOrNull() ?: return null
        if (atr <= 0) return null
        val n = closes.size
        val upNow = fast[n - 1] > slow[n - 1]
        val upPrev = fast[n - 2] > slow[n - 2]
        val slope = slow[n - 1] - slow[n - 6]
        val close = closes[n - 1]
        val structureLow = candles.takeLast(10).minOf { it.low }
        val structureHigh = candles.takeLast(10).maxOf { it.high }
        val separation = kotlin.math.abs(fast[n - 1] - slow[n - 1])

        return when {
            upNow && !upPrev && slope > 0 -> Draft(
                Direction.LONG, close, structureLow, (separation / (atr * 0.5)).coerceIn(0.0, 1.0),
                note = "EMA 9/21 cross up",
            )
            !upNow && upPrev && slope < 0 -> Draft(
                Direction.SHORT, close, structureHigh, (separation / (atr * 0.5)).coerceIn(0.0, 1.0),
                note = "EMA 9/21 cross down",
            )
            else -> null
        }
    }

    // ---------------- MACD Cross ----------------
    fun macdCross(candles: List<Candle>): Draft? {
        if (candles.size < 37) return null
        val closes = DoubleArray(candles.size) { candles[it].close }
        val fast = Indicators.ema(closes, 12)
        val slow = Indicators.ema(closes, 26)
        val macd = DoubleArray(closes.size) { fast[it] - slow[it] }
        val signal = Indicators.ema(macd, 9)
        val hist = DoubleArray(closes.size) { macd[it] - signal[it] }
        val atr = Indicators.atr(candles).lastOrNull() ?: return null
        if (atr <= 0) return null
        val n = closes.size
        val close = closes[n - 1]

        val crossUp = macd[n - 1] > signal[n - 1] && macd[n - 2] <= signal[n - 2]
        val crossDown = macd[n - 1] < signal[n - 1] && macd[n - 2] >= signal[n - 2]

        return when {
            crossUp && hist[n - 1] > 0 -> Draft(
                Direction.LONG, close, candles.takeLast(10).minOf { it.low },
                (kotlin.math.abs(hist[n - 1]) / atr).coerceIn(0.0, 1.0),
                note = "MACD cross up",
            )
            crossDown && hist[n - 1] < 0 -> Draft(
                Direction.SHORT, close, candles.takeLast(10).maxOf { it.high },
                (kotlin.math.abs(hist[n - 1]) / atr).coerceIn(0.0, 1.0),
                note = "MACD cross down",
            )
            else -> null
        }
    }

    // ---------------- Trend Structure ----------------
    fun trendStructure(candles: List<Candle>): Draft? {
        if (candles.size < 40) return null
        val atr = Indicators.atr(candles).lastOrNull() ?: return null
        if (atr <= 0) return null
        val highs = DoubleArray(candles.size) { candles[it].high }
        val lows = DoubleArray(candles.size) { candles[it].low }
        val (hi, lo) = Indicators.swings(highs, lows)
        if (hi.size < 3 || lo.size < 3) return null
        val close = candles.last().close

        fun higherHighs() = highs[hi.last()] > highs[hi[hi.size - 2]] &&
            highs[hi[hi.size - 2]] > highs[hi[hi.size - 3]]
        fun higherLows() = lows[lo.last()] > lows[lo[lo.size - 2]] &&
            lows[lo[lo.size - 2]] > lows[lo[lo.size - 3]]
        fun lowerHighs() = highs[hi.last()] < highs[hi[hi.size - 2]] &&
            highs[hi[hi.size - 2]] < highs[hi[hi.size - 3]]
        fun lowerLows() = lows[lo.last()] < lows[lo[lo.size - 2]] &&
            lows[lo[lo.size - 2]] < lows[lo[lo.size - 3]]

        val lastSwingHigh = highs[hi.last()]
        val lastSwingLow = lows[lo.last()]

        return when {
            higherHighs() && higherLows() && close > lastSwingHigh -> Draft(
                Direction.LONG, close, lastSwingLow,
                ((close - lastSwingLow) / (atr * 4)).coerceIn(0.0, 1.0),
                event = StructureEvent.BOS,
                note = "HH+HL, BOS $lastSwingHigh",
            )
            lowerHighs() && lowerLows() && close < lastSwingLow -> Draft(
                Direction.SHORT, close, lastSwingHigh,
                ((lastSwingHigh - close) / (atr * 4)).coerceIn(0.0, 1.0),
                event = StructureEvent.BOS,
                note = "LH+LL, BOS $lastSwingLow",
            )
            else -> null
        }
    }

    fun run(strategy: com.sniperrex.app.domain.model.CoreStrategy, candles: List<Candle>): Draft? =
        when (strategy) {
            com.sniperrex.app.domain.model.CoreStrategy.FVG -> fvg(candles)
            com.sniperrex.app.domain.model.CoreStrategy.ORDER_BLOCK_BOS -> obBos(candles)
            com.sniperrex.app.domain.model.CoreStrategy.EMA_CROSS -> emaCross(candles)
            com.sniperrex.app.domain.model.CoreStrategy.MACD_CROSS -> macdCross(candles)
            com.sniperrex.app.domain.model.CoreStrategy.TREND_STRUCTURE -> trendStructure(candles)
        }
}

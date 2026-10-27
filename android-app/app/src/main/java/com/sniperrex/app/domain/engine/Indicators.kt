package com.sniperrex.app.domain.engine

import kotlin.math.abs
import kotlin.math.max

/** Indikator implementasi sendiri — tidak ada library black-box. */
object Indicators {

    fun ema(src: DoubleArray, period: Int): DoubleArray {
        if (src.isEmpty()) return DoubleArray(0)
        val k = 2.0 / (period + 1)
        val out = DoubleArray(src.size)
        out[0] = src[0]
        for (i in 1 until src.size) out[i] = src[i] * k + out[i - 1] * (1 - k)
        return out
    }

    /** Wilder smoothing (dasar RSI & ATR). */
    fun rma(src: DoubleArray, period: Int): DoubleArray {
        if (src.isEmpty()) return DoubleArray(0)
        val alpha = 1.0 / period
        val out = DoubleArray(src.size)
        out[0] = src[0]
        for (i in 1 until src.size) out[i] = src[i] * alpha + out[i - 1] * (1 - alpha)
        return out
    }

    fun trueRange(candles: List<Candle>): DoubleArray {
        if (candles.isEmpty()) return DoubleArray(0)
        val out = DoubleArray(candles.size)
        out[0] = candles[0].high - candles[0].low
        for (i in 1 until candles.size) {
            val prevClose = candles[i - 1].close
            val h = candles[i].high
            val l = candles[i].low
            out[i] = max(h - l, max(abs(h - prevClose), abs(l - prevClose)))
        }
        return out
    }

    fun atr(candles: List<Candle>, period: Int = 14): DoubleArray = rma(trueRange(candles), period)

    fun rsi(closes: DoubleArray, period: Int = 14): DoubleArray {
        if (closes.isEmpty()) return DoubleArray(0)
        val gains = DoubleArray(closes.size)
        val losses = DoubleArray(closes.size)
        for (i in 1 until closes.size) {
            val delta = closes[i] - closes[i - 1]
            if (delta >= 0) gains[i] = delta else losses[i] = -delta
        }
        val avgGain = rma(gains, period)
        val avgLoss = rma(losses, period)
        val out = DoubleArray(closes.size)
        for (i in closes.indices) {
            out[i] = when {
                avgLoss[i] == 0.0 && avgGain[i] > 0 -> 100.0
                avgLoss[i] == 0.0 && avgGain[i] == 0 -> 50.0
                else -> 100.0 - (100.0 / (1.0 + avgGain[i] / avgLoss[i]))
            }
        }
        return out
    }

    fun volumeMa(volumes: DoubleArray, period: Int): DoubleArray {
        val out = DoubleArray(volumes.size)
        var sum = 0.0
        for (i in volumes.indices) {
            sum += volumes[i]
            if (i >= period) sum -= volumes[i - period]
            val count = minOf(i + 1, period)
            out[i] = sum / count
        }
        return out
    }

    /** Fractal swing — satu swing per plateau. */
    fun swings(highs: DoubleArray, lows: DoubleArray, lookback: Int = 2): Pair<List<Int>, List<Int>> {
        val hi = mutableListOf<Int>()
        val lo = mutableListOf<Int>()
        var lastHighIdx = -1_000_000
        var lastLowIdx = -1_000_000
        if (highs.size <= lookback * 2) return hi to lo
        for (i in lookback until highs.size - lookback) {
            var hMax = Double.NEGATIVE_INFINITY
            var lMin = Double.POSITIVE_INFINITY
            for (j in i - lookback..i + lookback) {
                if (highs[j] > hMax) hMax = highs[j]
                if (lows[j] < lMin) lMin = lows[j]
            }
            if (highs[i] == hMax && i - lastHighIdx > lookback) {
                hi.add(i); lastHighIdx = i
            }
            if (lows[i] == lMin && i - lastLowIdx > lookback) {
                lo.add(i); lastLowIdx = i
            }
        }
        return hi to lo
    }
}

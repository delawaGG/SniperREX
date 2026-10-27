package com.sniperrex.app.domain.engine

import com.sniperrex.app.domain.model.Direction
import com.sniperrex.app.domain.model.PairInfo
import com.sniperrex.app.domain.model.Pairs

/** 4 konfirmasi (spec §8). Semua mengembalikan nilai aktual untuk logging. */
object Filters {

    fun rsiCheck(candles: List<Candle>, direction: Direction, period: Int = 14, overbought: Double = 70.0, oversold: Double = 30.0): FilterCheck {
        val closes = DoubleArray(candles.size) { candles[it].close }
        val value = Indicators.rsi(closes, period).lastOrNull() ?: 50.0
        val passed: Boolean
        val threshold: Double
        if (direction == Direction.LONG) { passed = value < overbought; threshold = overbought }
        else { passed = value > oversold; threshold = oversold }
        return FilterCheck("RSI", passed, value, threshold, "RSI ${"%.1f".format(value)} vs ${threshold.toInt()}")
    }

    fun atrCheck(candles: List<Candle>, info: PairInfo): FilterCheck {
        val atr = Indicators.atr(candles).lastOrNull() ?: 0.0
        val price = candles.lastOrNull()?.close ?: return FilterCheck("ATR", false, 0.0, 0.0, "data kosong")
        val pct = if (price > 0) atr / price else 0.0
        val floor = Pairs.atrFloor(info)
        return FilterCheck(
            "ATR", pct > floor, pct, floor,
            "ATR ${"%.3f".format(pct * 100)}% vs floor ${"%.3f".format(floor * 100)}%",
        )
    }

    fun volumeCheck(candles: List<Candle>, period: Int = 20, mult: Double = 1.2): FilterCheck {
        val volumes = DoubleArray(candles.size) { candles[it].volume }
        val ma = Indicators.volumeMa(volumes, period).lastOrNull() ?: 0.0
        val vol = candles.lastOrNull()?.volume ?: 0.0
        val ratio = if (ma > 0) vol / ma else 0.0
        return FilterCheck("VOLUME", ratio > mult, ratio, mult, "vol/MA ${"%.2f".format(ratio)}x")
    }

    fun obCheck(candles: List<Candle>, entry: Double, zone: Pair<Double, Double>?, proximityAtr: Double = 1.0): FilterCheck {
        val atr = Indicators.atr(candles).lastOrNull() ?: 0.0
        val proximity = atr * proximityAtr
        if (zone != null) {
            val low = minOf(zone.first, zone.second)
            val high = maxOf(zone.first, zone.second)
            val mid = (low + high) / 2
            val dist = kotlin.math.abs(entry - mid)
            return FilterCheck("OB", dist <= proximity, dist, proximity,
                "jarak ke OB ${"%.5f".format(dist)} (<= ${"%.5f".format(proximity)})")
        }
        val from = maxOf(candles.size - 30, 0)
        for (i in candles.size - 1 downTo from) {
            val c = candles[i]
            if (c.close > c.open) {
                val mid = (c.low + c.high) / 2
                val dist = kotlin.math.abs(entry - mid)
                if (dist <= proximity) {
                    return FilterCheck("OB", true, dist, proximity,
                        "OB fallback bar -${candles.size - 1 - i}, jarak ${"%.5f".format(dist)}")
                }
            }
        }
        return FilterCheck("OB", false, -1.0, proximity, "tidak ada OB dalam 1 ATR")
    }

    fun run(
        selected: List<String>,
        candles: List<Candle>,
        info: PairInfo,
        direction: Direction,
        entry: Double,
        zone: Pair<Double, Double>?,
    ): List<FilterCheck> = selected.map { name ->
        when (name.uppercase()) {
            "RSI" -> rsiCheck(candles, direction)
            "ATR" -> atrCheck(candles, info)
            "VOLUME" -> volumeCheck(candles)
            "OB" -> obCheck(candles, entry, zone)
            else -> FilterCheck(name, false, 0.0, 0.0, "filter tidak dikenal")
        }
    }
}

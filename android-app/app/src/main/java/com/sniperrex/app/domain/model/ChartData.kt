package com.sniperrex.app.domain.model

import com.sniperrex.app.domain.engine.Candle

/**
 * Hasil permintaan chart. [stale] true berarti data berasal dari cache karena
 * request jaringan gagal — chart tetap tampil, tapi diberi label.
 */
data class ChartData(
    val pair: String,
    val timeframe: String,
    val candles: List<Candle> = emptyList(),
    val error: String? = null,
    val stale: Boolean = false,
) {
    val isEmpty: Boolean get() = candles.isEmpty()
}

package com.sniperrex.app.data.worker

import com.sniperrex.app.domain.model.CoreStrategy

/**
 * Preset default untuk scan latar belakang (spec §20).
 *
 * Kuota Twelve Data FREE = 8 kredit/menit DAN 800 kredit/hari.
 * 4 pair x 2 request tiap 15 menit = 768 kredit/hari (96% kuota) -> tidak aman
 * begitu user juga scan manual. Jadi worker memakai ROTASI 2 pair per siklus:
 * 4 kredit x 96 siklus = 384 kredit/hari, sisa kuota tetap ada.
 */
object ScanConfig {
    val ALL_PAIRS = listOf("XAUUSD", "EURUSD", "GBPUSD", "USDJPY")
    val CONFIRMATIONS = listOf("RSI", "ATR", "OB")
    const val TIMEFRAME = "M15"
    val STRATEGY = CoreStrategy.FVG

    const val INTERVAL_MIN = 15L
    val SLOT_MS = INTERVAL_MIN * 60_000L

    /** Pair untuk siklus ini: bergiliran setengah daftar tiap 15 menit. */
    fun pairsFor(nowMs: Long = System.currentTimeMillis()): List<String> {
        val slot = (nowMs / SLOT_MS) % 2
        return if (slot == 0L) ALL_PAIRS.take(2) else ALL_PAIRS.drop(2)
    }
}

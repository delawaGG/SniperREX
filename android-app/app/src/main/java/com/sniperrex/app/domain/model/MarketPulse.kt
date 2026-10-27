package com.sniperrex.app.domain.model

/**
 * Snapshot kondisi satu pair — dipakai Radar Market.
 * Bukan signal: hanya bias, tekanan, dan kualitas setup saat ini.
 */
data class MarketPulse(
    val pair: String,
    val price: Double,
    val changePct: Double,
    val rsi: Double,
    val atr: Double,
    val bias: String,
    val atrPct: Double,
    val state: PulseState,
) {
    /** Tekanan beli/jual kasar untuk bar indikator (0..1). */
    val pressure: Double get() = (rsi / 100.0).coerceIn(0.0, 1.0)
}

enum class PulseState(val label: String) {
    TRENDING_UP("trend naik"),
    TRENDING_DOWN("trend turun"),
    COMPRESSED("kompresi / menunggu"),
    EXHAUSTED("jenuh — hati-hati"),
    NO_DATA("belum ada data"),
}

package com.sniperrex.app.domain.model

/**
 * Metadata pair. pip != point (XAUUSD: pip 0.1, point 0.01).
 * `tdSymbol` = format Twelve Data (pakai slash).
 */
data class PairInfo(
    val symbol: String,
    val tdSymbol: String,
    val name: String,
    val category: String,
    val pipSize: Double,
    val pointSize: Double,
    val digits: Int,
)

object Pairs {
    val ALL: List<PairInfo> = listOf(
        PairInfo("XAUUSD", "XAU/USD", "Gold / USD", "Metal", 0.1, 0.01, 2),
        PairInfo("EURUSD", "EUR/USD", "Euro / USD", "Forex", 0.0001, 0.00001, 5),
        PairInfo("GBPUSD", "GBP/USD", "Pound / USD", "Forex", 0.0001, 0.00001, 5),
        PairInfo("USDJPY", "USD/JPY", "USD / Yen", "Forex", 0.01, 0.001, 3),
        PairInfo("BTCUSD", "BTC/USD", "Bitcoin / USD", "Crypto", 1.0, 0.01, 2),
    )

    fun of(symbol: String): PairInfo? = ALL.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }

    /** Floor ATR per kategori (v1.1) — JPY butuh ambang lebih rendah. */
    fun atrFloor(info: PairInfo): Double = when {
        info.symbol.endsWith("JPY") -> 0.0002
        info.category == "Metal" -> 0.0005
        info.category == "Forex" -> 0.0003
        else -> 0.0010
    }
}

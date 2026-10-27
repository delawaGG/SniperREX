package com.sniperrex.app.domain.repository

import com.sniperrex.app.domain.engine.Candle
import com.sniperrex.app.domain.model.AiVerdict
import com.sniperrex.app.domain.model.MarketPulse
import com.sniperrex.app.domain.model.ScanParams
import com.sniperrex.app.domain.model.ScanResult
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.SignalStatus
import kotlinx.coroutines.flow.Flow

interface SignalRepository {
    suspend fun scan(params: ScanParams): ScanResult
    fun observeSignals(): Flow<List<Signal>>
    fun observeSignal(id: String): Flow<Signal?>
    suspend fun updateStatus(id: String, status: SignalStatus)
    suspend fun analyze(signalId: String): AiVerdict
    suspend fun tierTimeframes(): List<String>
    suspend fun testTwelve(): Pair<Boolean, String>
    suspend fun testAi(): Pair<Boolean, String>

    /** Candle untuk chart native. Kosong bila key belum diisi atau permintaan gagal. */
    suspend fun candles(pair: String, timeframe: String, limit: Int = 160): List<Candle>

    /** Radar market: snapshot ringan tiap pair (1 request per pair). */
    suspend fun pulses(pairs: List<String>, timeframe: String = "M15"): List<MarketPulse>

    /**
     * Sweep semua pair x timeframe x strategi dalam satu putaran data.
     *
     * Kuota Twelve Data FREE = 8 kredit/menit, 800/hari. Satu putaran di sini
     * memakai 2 request per pair (TF utama + HTF M45) yang lalu dipakai ulang
     * oleh SEMUA strategi dan timeframe — jadi menambah strategi tidak menambah
     * kredit sama sekali. Itu sebabnya autoscan bisa menghasilkan banyak kandidat
     * tanpa membakar kuota user.
     */
    suspend fun sweep(): SweepReport
}

/** Ringkasan satu putaran sweep untuk ditampilkan di Dashboard. */
data class SweepReport(
    val scanned: Int,
    val found: List<Signal>,
    val skipped: Int,
    val message: String? = null,
)

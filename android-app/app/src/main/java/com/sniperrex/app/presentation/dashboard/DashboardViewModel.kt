package com.sniperrex.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperrex.app.domain.engine.Candle
import com.sniperrex.app.domain.model.ChartData
import com.sniperrex.app.domain.model.MarketPulse
import com.sniperrex.app.domain.model.Pairs
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.repository.SignalRepository
import com.sniperrex.app.domain.repository.SweepReport
import com.sniperrex.app.domain.usecase.AutoScanUseCase
import com.sniperrex.app.domain.usecase.ObserveSignalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State chart: candle terakhir + status pemuatan. */
data class ChartState(
    val pair: String = "XAUUSD",
    val timeframe: String = "M15",
    val candles: List<Candle> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /** Data berasal dari cache karena jaringan gagal — chart tetap tampil. */
    val stale: Boolean = false,
) {
    val last: Candle? get() = candles.lastOrNull()
    val prev: Candle? get() = candles.dropLast(1).lastOrNull()
    val hasData: Boolean get() = candles.size >= 2
    val changePct: Double
        get() {
            val l = last ?: return 0.0
            val p = prev ?: return 0.0
            return if (p.close > 0) (l.close - p.close) / p.close * 100.0 else 0.0
        }
}

data class RadarState(
    val pulses: List<MarketPulse> = emptyList(),
    val loading: Boolean = false,
)

data class SweepUi(
    val running: Boolean = false,
    val message: String? = null,
    val newSignals: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: SignalRepository,
    observeSignals: ObserveSignalsUseCase,
    private val autoScan: AutoScanUseCase,
) : ViewModel() {

    private val _chart = MutableStateFlow(ChartState())
    val chart: StateFlow<ChartState> = _chart.asStateFlow()

    private val _radar = MutableStateFlow(RadarState())
    val radar: StateFlow<RadarState> = _radar.asStateFlow()

    private val _sweep = MutableStateFlow(SweepUi())
    val sweep: StateFlow<SweepUi> = _sweep.asStateFlow()

    val signals: StateFlow<List<Signal>> = observeSignals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Chart terakhir yang berhasil dimuat. Dipakai sebagai cadangan antar navigasi. */
    private val lastGood = mutableMapOf<String, ChartData>()

    init {
        viewModelScope.launch { runCatching { autoScan.restore() } }
        select("XAUUSD", "M15")
        refreshRadar()
    }

    fun select(pair: String, timeframe: String = _chart.value.timeframe) {
        // Tampilkan data terakhir untuk pair/TF ini segera, tanpa menunggu jaringan.
        val key = "$pair|$timeframe"
        val backup = lastGood[key]
        _chart.update {
            it.copy(
                pair = pair,
                timeframe = timeframe,
                candles = backup?.candles.orEmpty(),
                stale = backup != null,
                error = null,
            )
        }
        loadCandles()
    }

    fun selectTimeframe(timeframe: String) = select(_chart.value.pair, timeframe)

    fun loadCandles() {
        val pair = _chart.value.pair
        val timeframe = _chart.value.timeframe
        _chart.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            val data = runCatching { repo.candles(pair, timeframe, 160) }.getOrNull()

            _chart.update { current ->
                // Abaikan hasil lama bila user sudah memilih pair/TF lain.
                if (current.pair != pair || current.timeframe != timeframe) return@update current

                val candles = data?.candles.orEmpty()
                if (candles.size >= 2) {
                    lastGood["$pair|$timeframe"] = data!!
                    current.copy(
                        candles = candles,
                        loading = false,
                        stale = data.stale,
                        error = if (data.stale) data.error else null,
                    )
                } else {
                    current.copy(
                        loading = false,
                        error = data?.error ?: "chart kosong — data belum tersedia",
                    )
                }
            }
        }
    }

    /**
     * Radar dimuat bertahap: satu pair per request, hasil langsung ditampilkan.
     * Dengan gerbang kuota 8 detik/request, menunggu 4 pair sekaligus = 32 detik
     * layar kosong. Bertahap membuat baris muncul satu per satu.
     */
    fun refreshRadar() {
        if (_radar.value.loading) return
        _radar.update { it.copy(loading = true) }
        viewModelScope.launch {
            val symbols = Pairs.ALL.filter { it.symbol != "BTCUSD" }.map { it.symbol }
            val collected = mutableListOf<MarketPulse>()
            for (symbol in symbols) {
                val one = runCatching { repo.pulses(listOf(symbol)) }.getOrNull().orEmpty()
                if (one.isNotEmpty()) {
                    collected += one
                    val snapshot = collected.toList()
                    _radar.update { it.copy(pulses = snapshot) }
                }
            }
            _radar.update { it.copy(loading = false) }
        }
    }

    /** Scan manual semua pair dengan engine yang sama seperti autoscan. */
    fun runSweep() {
        if (_sweep.value.running) return
        _sweep.update { it.copy(running = true, message = null) }
        viewModelScope.launch {
            val report: SweepReport? = runCatching { repo.sweep() }.getOrNull()
            _sweep.update {
                it.copy(
                    running = false,
                    message = report?.message ?: "sweep gagal",
                    newSignals = report?.found?.size ?: 0,
                )
            }
            loadCandles()
        }
    }
}

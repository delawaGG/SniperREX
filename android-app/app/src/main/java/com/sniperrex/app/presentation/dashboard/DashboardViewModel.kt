package com.sniperrex.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperrex.app.domain.engine.Candle
import com.sniperrex.app.domain.model.MarketPulse
import com.sniperrex.app.domain.model.Pairs
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.repository.SweepReport
import com.sniperrex.app.domain.usecase.AutoScanUseCase
import com.sniperrex.app.domain.usecase.ObserveSignalsUseCase
import com.sniperrex.app.domain.repository.SignalRepository
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
) {
    val last: Candle? get() = candles.lastOrNull()
    val prev: Candle? get() = candles.dropLast(1).lastOrNull()
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

    init {
        // WorkManager bisa hilang setelah reboot/force stop -> sinkronkan lagi.
        viewModelScope.launch { runCatching { autoScan.restore() } }
        select("XAUUSD", "M15")
        refreshRadar()
    }

    fun select(pair: String, timeframe: String = _chart.value.timeframe) {
        _chart.update { it.copy(pair = pair, timeframe = timeframe) }
        loadCandles()
    }

    fun selectTimeframe(timeframe: String) = select(_chart.value.pair, timeframe)

    fun loadCandles() {
        val snapshot = _chart.value
        _chart.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val data = runCatching {
                repo.candles(snapshot.pair, snapshot.timeframe, 160)
            }.getOrNull().orEmpty()

            _chart.update {
                it.copy(
                    candles = data,
                    loading = false,
                    error = when {
                        data.isNotEmpty() -> null
                        it.pair != snapshot.pair || it.timeframe != snapshot.timeframe -> null
                        else -> "chart kosong — pastikan API key terisi dan kuota Twelve Data tersisa"
                    },
                )
            }
        }
    }

    fun refreshRadar() {
        _radar.update { it.copy(loading = true) }
        viewModelScope.launch {
            val data = runCatching {
                repo.pulses(Pairs.ALL.filter { it.symbol != "BTCUSD" }.map { it.symbol })
            }.getOrNull().orEmpty()
            _radar.update { it.copy(pulses = data, loading = false) }
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
            refreshRadar()
            loadCandles()
        }
    }
}

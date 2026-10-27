package com.sniperrex.app.presentation.strategy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperrex.app.domain.model.CoreStrategy
import com.sniperrex.app.domain.model.ScanParams
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.usecase.LoadTimeframesUseCase
import com.sniperrex.app.domain.usecase.ScanMarketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUi(
    val busy: Boolean = false,
    val message: String? = null,
    val warnings: List<String> = emptyList(),
    val signal: Signal? = null,
)

@HiltViewModel
class StrategyViewModel @Inject constructor(
    private val scanMarket: ScanMarketUseCase,
    loadTimeframes: LoadTimeframesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUi())
    val state: StateFlow<ScanUi> = _state.asStateFlow()

    private val _timeframes = MutableStateFlow(listOf("M15", "M30", "M45"))
    val timeframes: StateFlow<List<String>> = _timeframes.asStateFlow()

    init {
        viewModelScope.launch { _timeframes.value = loadTimeframes() }
    }

    fun scan(pair: String, timeframe: String, strategy: CoreStrategy, confirmations: List<String>) =
        viewModelScope.launch {
            _state.value = ScanUi(busy = true)
            val result = runCatching {
                scanMarket(ScanParams(pair, timeframe, strategy, confirmations))
            }
            _state.value = result.fold(
                onSuccess = { r ->
                    ScanUi(signal = r.signal, message = r.reason ?: "Signal ditemukan", warnings = r.warnings)
                },
                onFailure = { e -> ScanUi(message = "Gagal: ${e.message}") },
            )
        }
}

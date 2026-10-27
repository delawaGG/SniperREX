package com.sniperrex.app.presentation.signals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.SignalStatus
import com.sniperrex.app.domain.usecase.AnalyzeSignalUseCase
import com.sniperrex.app.domain.usecase.ObserveSignalUseCase
import com.sniperrex.app.domain.usecase.UpdateSignalStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignalDetailViewModel @Inject constructor(
    observeSignal: ObserveSignalUseCase,
    private val analyzeSignal: AnalyzeSignalUseCase,
    private val updateStatus: UpdateSignalStatusUseCase,
) : ViewModel() {

    private val _analyzing = MutableStateFlow(false)
    val analyzing: StateFlow<Boolean> = _analyzing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signal(id: String): StateFlow<Signal?> = observeSignal(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun analyze(id: String) = viewModelScope.launch {
        _analyzing.value = true
        runCatching { analyzeSignal(id) }
            .onFailure { _error.value = it.message ?: "AI gagal" }
        _analyzing.value = false
    }

    fun skip(id: String) = viewModelScope.launch {
        runCatching { updateStatus(id, SignalStatus.SKIPPED) }
            .onFailure { _error.value = it.message ?: "Gagal update status" }
    }
}

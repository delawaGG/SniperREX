package com.sniperrex.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.usecase.AutoScanUseCase
import com.sniperrex.app.domain.usecase.ObserveSignalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeSignals: ObserveSignalsUseCase,
    autoScan: AutoScanUseCase,
) : ViewModel() {

    init {
        // WorkManager bisa hilang setelah reboot/force stop -> sinkronkan lagi.
        viewModelScope.launch { runCatching { autoScan.restore() } }
    }

    val signals: StateFlow<List<Signal>> = observeSignals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

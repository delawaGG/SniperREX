package com.sniperrex.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperrex.app.data.local.Credentials
import com.sniperrex.app.domain.usecase.AutoScanUseCase
import com.sniperrex.app.domain.usecase.ObserveCredentialsUseCase
import com.sniperrex.app.domain.usecase.SaveCredentialsUseCase
import com.sniperrex.app.domain.usecase.TestConnectionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUi(
    val twelveKey: String = "",
    val aiKey: String = "",
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val twelveStatus: String? = null,
    val aiStatus: String? = null,
    val busy: Boolean = false,
    val autoScan: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val saveCredentials: SaveCredentialsUseCase,
    private val testConnections: TestConnectionsUseCase,
    private val autoScan: AutoScanUseCase,
    observeCredentials: ObserveCredentialsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUi())
    val state: StateFlow<SettingsUi> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            autoScan.observe().collect { enabled ->
                _state.value = _state.value.copy(autoScan = enabled)
            }
        }
        viewModelScope.launch {
            observeCredentials().collect { c ->
                _state.value = _state.value.copy(
                    twelveKey = c.twelveKey, aiKey = c.aiKey,
                    aiBaseUrl = c.aiBaseUrl, aiModel = c.aiModel,
                )
            }
        }
    }

    fun onTwelveKey(value: String) = _state.update { it.copy(twelveKey = value) }
    fun onAiKey(value: String) = _state.update { it.copy(aiKey = value) }
    fun onAiBase(value: String) = _state.update { it.copy(aiBaseUrl = value) }
    fun onAiModel(value: String) = _state.update { it.copy(aiModel = value) }

    fun setAutoScan(enabled: Boolean) = viewModelScope.launch {
        runCatching { autoScan.setEnabled(enabled) }
            .onFailure { _state.value = _state.value.copy(twelveStatus = "Gagal mengatur jadwal") }
    }

    fun scanNow() {
        autoScan.runNow()
        _state.value = _state.value.copy(twelveStatus = "Scan latar belakang dimulai")
    }

    fun save() = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val c = _state.value
        saveCredentials(Credentials(c.twelveKey.trim(), c.aiKey.trim(), c.aiBaseUrl.trim(), c.aiModel.trim()))
        _state.update { it.copy(busy = false, twelveStatus = "Kredensial disimpan di perangkat") }
    }

    fun testTwelve() = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val (ok, detail) = testConnections.twelve()
        _state.update { it.copy(busy = false, twelveStatus = (if (ok) "OK: " else "Gagal: ") + detail) }
    }

    fun testAi() = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val (ok, detail) = testConnections.ai()
        _state.update { it.copy(busy = false, aiStatus = (if (ok) "OK: " else "Gagal: ") + detail) }
    }
}

private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}

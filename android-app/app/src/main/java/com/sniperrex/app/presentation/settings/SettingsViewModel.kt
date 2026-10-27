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
    val saveStatus: String? = null,
    val twelveOk: Boolean? = null,
    val aiOk: Boolean? = null,
    val revealTwelve: Boolean = false,
    val revealAi: Boolean = false,
    val savedHint: String = "belum ada key tersimpan",
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

    /**
     * Setelah user mulai mengetik, aliran DataStore tidak boleh menimpa isi field.
     * Tanpa gerbang ini setiap emisi DataStore mengembalikan field ke nilai lama
     * sehingga terasa "tidak terbaca".
     */
    private var edited = false

    init {
        viewModelScope.launch {
            autoScan.observe().collect { enabled ->
                _state.value = _state.value.copy(autoScan = enabled)
            }
        }
        viewModelScope.launch {
            observeCredentials().collect { c ->
                val hint = when {
                    c.twelveKey.isBlank() -> "belum ada key tersimpan"
                    else -> "tersimpan: ••••" + c.twelveKey.takeLast(4)
                }
                if (!edited) {
                    _state.value = _state.value.copy(
                        twelveKey = c.twelveKey,
                        aiKey = c.aiKey,
                        aiBaseUrl = c.aiBaseUrl,
                        aiModel = c.aiModel,
                    )
                }
                _state.value = _state.value.copy(savedHint = hint)
            }
        }
    }

    fun onTwelveKey(value: String) {
        edited = true
        _state.value = _state.value.copy(twelveKey = value, twelveOk = null, twelveStatus = null)
    }

    fun onAiKey(value: String) {
        edited = true
        _state.value = _state.value.copy(aiKey = value, aiOk = null, aiStatus = null)
    }

    fun onAiBase(value: String) {
        edited = true
        _state.value = _state.value.copy(aiBaseUrl = value)
    }

    fun onAiModel(value: String) {
        edited = true
        _state.value = _state.value.copy(aiModel = value)
    }

    fun toggleRevealTwelve() =
        _state.update { it.copy(revealTwelve = !it.revealTwelve) }

    fun toggleRevealAi() =
        _state.update { it.copy(revealAi = !it.revealAi) }

    fun setAutoScan(enabled: Boolean) = viewModelScope.launch {
        runCatching { autoScan.setEnabled(enabled) }
            .onFailure {
                _state.value = _state.value.copy(saveStatus = "Gagal mengatur jadwal: ${it.message}")
            }
    }

    fun scanNow() {
        autoScan.runNow()
        _state.value = _state.value.copy(saveStatus = "Scan latar belakang dimulai")
    }

    /** Simpan + laporkan hasil nyata (bukan asumsi berhasil). */
    fun save() = viewModelScope.launch {
        _state.update { it.copy(busy = true, saveStatus = null) }
        val c = _state.value
        val outcome = runCatching { persist(c) }
        edited = false
        _state.update { current ->
            outcome.fold(
                onSuccess = {
                    current.copy(
                        busy = false,
                        saveStatus = "Tersimpan di perangkat",
                        savedHint = hintOf(c.twelveKey),
                    )
                },
                onFailure = { e ->
                    current.copy(
                        busy = false,
                        saveStatus = "Gagal menyimpan: " + (e.message ?: e.javaClass.simpleName),
                    )
                },
            )
        }
    }

    /**
     * Uji koneksi memakai key yang SEDANG DIKETIK, bukan yang tersimpan.
     * Key disimpan dulu supaya scan berikutnya ikut memakainya.
     */
    fun testTwelve() = viewModelScope.launch {
        _state.update { it.copy(busy = true, twelveStatus = "menguji...", twelveOk = null) }
        val typed = _state.value.twelveKey.trim()
        if (typed.isEmpty()) {
            _state.update { it.copy(busy = false, twelveOk = false, twelveStatus = "Isi API key dulu, lalu tekan TEST lagi") }
            return@launch
        }
        val persistError = runCatching { persist(_state.value) }.exceptionOrNull()
        if (persistError != null) {
            _state.update {
                it.copy(
                    busy = false, twelveOk = false,
                    twelveStatus = "Key gagal disimpan: ${persistError.message ?: persistError.javaClass.simpleName}",
                )
            }
            return@launch
        }
        edited = false
        val (ok, detail) = testConnections.twelve()
        _state.update {
            it.copy(
                busy = false, twelveOk = ok,
                twelveStatus = (if (ok) "OK · " else "Gagal · ") + detail,
                savedHint = hintOf(typed),
                saveStatus = if (ok) "Key tersimpan di perangkat" else it.saveStatus,
            )
        }
    }

    fun testAi() = viewModelScope.launch {
        _state.update { it.copy(busy = true, aiStatus = "menguji...", aiOk = null) }
        if (_state.value.aiKey.trim().isEmpty()) {
            _state.update { it.copy(busy = false, aiOk = false, aiStatus = "Isi AI key dulu (opsional, boleh dilewati)") }
            return@launch
        }
        runCatching { persist(_state.value) }
        edited = false
        val (ok, detail) = testConnections.ai()
        _state.update {
            it.copy(busy = false, aiOk = ok, aiStatus = (if (ok) "OK · " else "Gagal · ") + detail)
        }
    }

    private suspend fun persist(c: SettingsUi) {
        saveCredentials(
            Credentials(
                twelveKey = c.twelveKey.trim(),
                aiKey = c.aiKey.trim(),
                aiBaseUrl = c.aiBaseUrl.trim(),
                aiModel = c.aiModel.trim(),
            )
        )
    }

    private fun hintOf(key: String): String =
        if (key.isBlank()) "belum ada key tersimpan" else "tersimpan: ••••" + key.trim().takeLast(4)
}

private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}

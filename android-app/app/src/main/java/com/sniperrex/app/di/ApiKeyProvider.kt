package com.sniperrex.app.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menyimpan kredensial user di memori (sumber asli: DataStore).
 * Tidak ada key yang di-hardcode di aplikasi — user mengisi sendiri.
 */
@Singleton
class ApiKeyProvider @Inject constructor() {
    private val twelveKey = MutableStateFlow<String?>(null)
    private val aiKey = MutableStateFlow<String?>(null)
    private val aiBaseUrl = MutableStateFlow<String?>(null)
    private val aiModel = MutableStateFlow<String?>(null)

    fun twelveState(): StateFlow<String?> = twelveKey
    fun aiState(): StateFlow<String?> = aiKey

    fun setTwelveKey(value: String?) { twelveKey.value = value?.takeIf { it.isNotBlank() } }
    fun setAi(key: String?, baseUrl: String?, model: String?) {
        aiKey.value = key?.takeIf { it.isNotBlank() }
        aiBaseUrl.value = baseUrl?.takeIf { it.isNotBlank() }
        aiModel.value = model?.takeIf { it.isNotBlank() }
    }

    fun currentTwelve(): String? = twelveKey.value
    fun currentAi(): String? = aiKey.value
    fun currentAiBase(): String? = aiBaseUrl.value
    fun currentAiModel(): String? = aiModel.value

    fun hasTwelveKey(): Boolean = !twelveKey.value.isNullOrBlank()
}

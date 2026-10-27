package com.sniperrex.app.di

import com.sniperrex.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base URL backend bisa diubah user saat runtime.
 * Emulator: http://10.0.2.2:8000/ — perangkat nyata: IP LAN mesin backend.
 */
@Singleton
class BackendUrlProvider @Inject constructor() {
    private val url = MutableStateFlow(BuildConfig.BASE_URL)
    fun current(): String = url.value
    fun set(value: String?) {
        val clean = value?.trim().orEmpty()
        if (clean.isBlank()) return
        url.value = if (clean.endsWith("/")) clean else "$clean/"
    }
}

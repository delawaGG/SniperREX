package com.sniperrex.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Credentials(
    val twelveKey: String = "",
    val aiKey: String = "",
    val aiBaseUrl: String = "",
    val aiModel: String = "",
)

/** Kredensial milik user di perangkat. Tidak pernah dikirim ke disk server. */
@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val twelve = stringPreferencesKey("twelve_key")
        val aiKey = stringPreferencesKey("ai_key")
        val aiBase = stringPreferencesKey("ai_base_url")
        val aiModel = stringPreferencesKey("ai_model")
        val disclaimerSeen = stringPreferencesKey("disclaimer_seen")
        val autoScan = stringPreferencesKey("auto_scan")
    }

    val credentials: Flow<Credentials> = dataStore.data.map { prefs ->
        Credentials(
            twelveKey = prefs[Keys.twelve].orEmpty(),
            aiKey = prefs[Keys.aiKey].orEmpty(),
            aiBaseUrl = prefs[Keys.aiBase].orEmpty(),
            aiModel = prefs[Keys.aiModel].orEmpty(),
        )
    }

    val disclaimerSeen: Flow<Boolean> = dataStore.data.map { it[Keys.disclaimerSeen] == "true" }

    val autoScan: Flow<Boolean> = dataStore.data.map { it[Keys.autoScan] == "true" }

    suspend fun autoScanEnabled(): Boolean = autoScan.first()

    suspend fun setAutoScan(enabled: Boolean) {
        dataStore.edit { it[Keys.autoScan] = enabled.toString() }
    }

    suspend fun current(): Credentials = credentials.first()

    suspend fun save(credentials: Credentials) {
        dataStore.edit { prefs ->
            prefs[Keys.twelve] = credentials.twelveKey
            prefs[Keys.aiKey] = credentials.aiKey
            prefs[Keys.aiBase] = credentials.aiBaseUrl
            prefs[Keys.aiModel] = credentials.aiModel
        }
    }

    suspend fun markDisclaimerSeen() {
        dataStore.edit { it[Keys.disclaimerSeen] = "true" }
    }
}

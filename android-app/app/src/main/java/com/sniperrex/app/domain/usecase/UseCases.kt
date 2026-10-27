package com.sniperrex.app.domain.usecase

import com.sniperrex.app.data.local.Credentials
import com.sniperrex.app.data.worker.ScanScheduler
import com.sniperrex.app.data.local.SettingsStore
import com.sniperrex.app.domain.model.AiVerdict
import com.sniperrex.app.domain.model.ScanParams
import com.sniperrex.app.domain.model.ScanResult
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.SignalStatus
import com.sniperrex.app.domain.repository.SignalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanMarketUseCase @Inject constructor(private val repo: SignalRepository) {
    suspend operator fun invoke(params: ScanParams): ScanResult = repo.scan(params)
}

class ObserveSignalsUseCase @Inject constructor(private val repo: SignalRepository) {
    operator fun invoke(): Flow<List<Signal>> = repo.observeSignals()
}

class ObserveSignalUseCase @Inject constructor(private val repo: SignalRepository) {
    operator fun invoke(id: String): Flow<Signal?> = repo.observeSignal(id)
}

class AnalyzeSignalUseCase @Inject constructor(private val repo: SignalRepository) {
    suspend operator fun invoke(signalId: String): AiVerdict = repo.analyze(signalId)
}

class UpdateSignalStatusUseCase @Inject constructor(private val repo: SignalRepository) {
    suspend operator fun invoke(id: String, status: SignalStatus) = repo.updateStatus(id, status)
}

class LoadTimeframesUseCase @Inject constructor(private val repo: SignalRepository) {
    suspend operator fun invoke(): List<String> = runCatching { repo.tierTimeframes() }
        .getOrDefault(listOf("M15"))
}

class TestConnectionsUseCase @Inject constructor(
    private val repo: SignalRepository,
    private val settingsStore: SettingsStore,
) {
    suspend fun twelve(): Pair<Boolean, String> = repo.testTwelve()
    suspend fun ai(): Pair<Boolean, String> = repo.testAi()
}

class SaveCredentialsUseCase @Inject constructor(private val settingsStore: SettingsStore) {
    suspend operator fun invoke(credentials: Credentials) = settingsStore.save(credentials)
}

class ObserveCredentialsUseCase @Inject constructor(private val settingsStore: SettingsStore) {
    operator fun invoke(): Flow<Credentials> = settingsStore.credentials
}

/** Toggle auto-scan 15 menit; menyentuh dua sisi: DataStore + WorkManager. */
class AutoScanUseCase @Inject constructor(
    private val settingsStore: SettingsStore,
    private val scheduler: ScanScheduler,
) {
    fun observe(): Flow<Boolean> = settingsStore.autoScan

    suspend fun setEnabled(enabled: Boolean) {
        settingsStore.setAutoScan(enabled)
        if (enabled) scheduler.schedule() else scheduler.cancel()
    }

    /** Pulihkan jadwal saat app dibuka (setelah reboot / force stop). */
    suspend fun restore() {
        if (settingsStore.autoScanEnabled()) scheduler.schedule() else scheduler.cancel()
    }

    fun runNow() = scheduler.runNow()
}

class DisclaimerAckUseCase @Inject constructor(private val settingsStore: SettingsStore) {
    fun observe(): Flow<Boolean> = settingsStore.disclaimerSeen
    suspend fun ack() = settingsStore.markDisclaimerSeen()
}

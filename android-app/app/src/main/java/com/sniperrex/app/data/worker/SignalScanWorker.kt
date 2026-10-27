package com.sniperrex.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sniperrex.app.data.local.SettingsStore
import com.sniperrex.app.domain.repository.SignalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Scan latar belakang tiap 15 menit.
 *
 * Memakai `sweep()`: satu putaran data per pair/timeframe dipakai ulang oleh
 * semua strategi, jadi jumlah kandidat naik tanpa menambah kredit Twelve Data.
 * Pagar: tidak jalan tanpa API key, atau saat toggle auto-scan mati.
 */
@HiltWorker
class SignalScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: SignalRepository,
    private val settings: SettingsStore,
    private val notifier: ScanNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val credentials = settings.current()
        if (credentials.twelveKey.isBlank()) return Result.success()
        if (!settings.autoScanEnabled()) return Result.success()

        val report = runCatching { repository.sweep() }.getOrNull()
            ?: return Result.success()

        // Notifikasi dibatasi: satu ringkasan, bukan satu per signal, supaya
        // tidak jadi spam saat sweep menemukan banyak setup sekaligus.
        report.found.take(3).forEach { notifier.notifySignal(it) }

        return Result.success()
    }
}

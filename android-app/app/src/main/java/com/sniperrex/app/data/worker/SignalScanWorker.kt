package com.sniperrex.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sniperrex.app.data.local.SettingsStore
import com.sniperrex.app.domain.model.ScanParams
import com.sniperrex.app.domain.repository.SignalRepository
import retrofit2.HttpException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Scan latar belakang tiap 15 menit.
 *
 * Pagar:
 * - Tidak jalan bila user belum mengisi API key (aplikasi dibagikan tanpa key).
 * - Tidak jalan bila toggle auto-scan mati.
 * - Signal hanya dievaluasi di bar tertutup terakhir; duplikat ditolak server.
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

        var found = 0
        var rateLimited = false
        // Server dedupe bar yang sama -> cegah notifikasi berulang tiap 15 menit.
        val freshCutoff = System.currentTimeMillis() - FRESH_WINDOW_MS

        for (pair in ScanConfig.pairsFor()) {
            val result = runCatching {
                repository.scan(
                    ScanParams(
                        pair = pair,
                        timeframe = ScanConfig.TIMEFRAME,
                        coreStrategy = ScanConfig.STRATEGY,
                        confirmations = ScanConfig.CONFIRMATIONS,
                    )
                )
            }
            result.onSuccess { scanResult ->
                scanResult.signal
                    ?.takeIf { it.createdAtEpochMs >= freshCutoff }
                    ?.let { signal ->
                        found++
                        notifier.notifySignal(signal)
                    }
            }.onFailure { error ->
                val code = (error as? HttpException)?.code()
                if (code == 429 || error.message?.contains("rate limit", ignoreCase = true) == true) {
                    rateLimited = true
                }
            }
            if (isStopped) break
        }

        return when {
            rateLimited -> Result.retry()
            found == 0 -> Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val FRESH_WINDOW_MS = 20 * 60_000L
    }
}

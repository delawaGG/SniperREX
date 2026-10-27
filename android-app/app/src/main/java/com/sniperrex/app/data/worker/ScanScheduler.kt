package com.sniperrex.app.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val workManager get() = WorkManager.getInstance(context)

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<SignalScanWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag(TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Trigger sekali jalan (tombol "Scan sekarang"). */
    fun runNow() {
        val request = androidx.work.OneTimeWorkRequestBuilder<SignalScanWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(TAG)
            .build()
        workManager.enqueue(request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_NAME)
    }

    companion object {
        const val INTERVAL_MINUTES = 15L     // minimum WorkManager
        const val UNIQUE_NAME = "signal_scan_periodic"
        const val TAG = "signal_scan"
    }
}

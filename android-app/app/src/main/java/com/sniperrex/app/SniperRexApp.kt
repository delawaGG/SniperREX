package com.sniperrex.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SniperRexApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Dipasang sebelum Hilt/WorkManager init agar crash paling awal pun tercatat. */
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        CrashGuard.install(base, BuildConfig.VERSION_CODE)
    }

    override fun onCreate() {
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}

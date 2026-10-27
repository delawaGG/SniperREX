package com.sniperrex.app.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sniperrex.app.R
import com.sniperrex.app.domain.model.Signal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Signal SniperRex",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Signal baru dari scan latar belakang" }
        manager.createNotificationChannel(channel)
    }

    fun notifySignal(signal: Signal) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel()
        val text = "${signal.pair} ${signal.timeframe} ${signal.direction.name} · " +
            "score ${signal.quantScore} · RR ${signal.riskReward}"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("Signal ${signal.pair}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        manager.notify(signal.id.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "sniperrex_signals"
    }
}

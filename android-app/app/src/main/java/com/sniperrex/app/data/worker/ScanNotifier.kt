package com.sniperrex.app.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sniperrex.app.MainActivity
import com.sniperrex.app.R
import com.sniperrex.app.domain.model.Direction
import com.sniperrex.app.domain.model.Signal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifikasi signal hasil scan.
 *
 * Tiga hal yang dulu membuat notifikasi tidak pernah muncul — semuanya
 * diperbaiki di sini:
 * 1. Izin POST_NOTIFICATIONS tidak pernah diminta ke user (Android 13+ default
 *    menolak), sehingga `notify()` dibatalkan diam-diam.
 * 2. Kanal memakai IMPORTANCE_DEFAULT tanpa getaran/heads-up, mudah terlewat.
 * 3. Signal ditemukan tapi hanya dikirim satu per satu tanpa ringkasan, jadi
 *    tidak ada satu notifikasi pun yang bisa diketuk untuk membuka app.
 */
@Singleton
class ScanNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    /** Apakah izin notifikasi sudah diberikan user. */
    fun canNotify(): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // IMPORTANCE_HIGH -> heads-up + getaran, supaya signal tidak terlewat.
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Signal SniperRex",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Setup baru dari scan otomatis dan scan manual"
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun arrow(direction: Direction) =
        if (direction == Direction.LONG) "▲ BUY" else "▼ SELL"

    /** Satu signal: notifikasi detail. */
    fun notifySignal(signal: Signal) {
        if (!canNotify()) return
        ensureChannel()

        val title = "${arrow(signal.direction)} ${signal.pair} · ${signal.timeframe}"
        val body = buildString {
            append("Entry ${fmt(signal.entry)}  ")
            append("SL ${fmt(signal.stopLoss)}  ")
            append("TP ${fmt(signal.takeProfits.first())}")
            append("\nRR 1:${signal.riskReward} · skor ${signal.quantScore}")
        }

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

        runCatching { manager.notify(signal.id.hashCode(), n) }
    }

    /**
     * Beberapa signal sekaligus: satu notifikasi ringkasan agar tidak spam,
     * tapi tetap memberi tahu jumlah dan setup terkuat.
     */
    fun notifySummary(signals: List<Signal>) {
        if (signals.isEmpty() || !canNotify()) return
        ensureChannel()

        val best = signals.maxByOrNull { it.quantScore } ?: return
        val title = "${signals.size} setup baru terdeteksi"
        val body = buildString {
            append("${arrow(best.direction)} ${best.pair} ${best.timeframe}")
            append(" · skor ${best.quantScore} · RR 1:${best.riskReward}")
            if (signals.size > 1) {
                append("\n+${signals.size - 1} setup lain: ")
                append(
                    signals.filter { it.id != best.id }
                        .take(3)
                        .joinToString(", ") { "${it.pair} ${it.timeframe}" },
                )
            }
        }

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

        runCatching { manager.notify(SUMMARY_ID, n) }
    }

    /** Untuk tombol "Tes notifikasi" di Settings. */
    fun notifyTest(): Boolean {
        if (!canNotify()) return false
        ensureChannel()
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Notifikasi aktif")
            .setContentText("SniperRex akan memberi tahu saat scan menemukan setup.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "SniperRex akan memberi tahu saat scan menemukan setup. " +
                        "Kalau pesan ini muncul, izin dan kanal sudah benar.",
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        return runCatching { manager.notify(TEST_ID, n) }.isSuccess
    }

    private fun fmt(v: Double): String =
        if (v >= 1000) String.format("%.1f", v) else String.format("%.5f", v).trimEnd('0').trimEnd('.')

    companion object {
        const val CHANNEL_ID = "sniperrex_signals"
        private const val SUMMARY_ID = 9001
        private const val TEST_ID = 9002
    }
}

package com.sniperrex.app

import android.content.Context
import java.io.File

/**
 * Menyimpan stacktrace crash terakhir ke file internal, supaya bisa dibaca
 * di layar berikutnya tanpa perlu logcat / adb.
 *
 * Setiap log diberi cap versi. Log tanpa cap atau dari versi lain dianggap
 * basi dan dibuang, supaya layar diagnostik tidak menampilkan crash lama.
 */
object CrashGuard {

    private const val FILE_NAME = "last_crash.txt"
    private const val TAG_PREFIX = "v="

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun install(context: Context, versionCode: Int) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val header = TAG_PREFIX + versionCode + "\n" +
                    "thread=" + thread.name + "\n" +
                    "device=" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL +
                    " sdk=" + android.os.Build.VERSION.SDK_INT + "\n\n"
                file(context).writeText(header + error.stackTraceToString())
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Versi yang tercatat di log crash, atau null kalau tidak ada/format lama. */
    fun loggedVersion(raw: String): Int? {
        val first = raw.lineSequence().firstOrNull().orEmpty().trim()
        if (!first.startsWith(TAG_PREFIX)) return null
        return first.removePrefix(TAG_PREFIX).trim().toIntOrNull()
    }

    fun read(context: Context, versionCode: Int): String? {
        val raw = runCatching { file(context).takeIf { it.exists() }?.readText() }.getOrNull()
        if (raw.isNullOrBlank()) return null
        // Hanya tampilkan crash dari build yang sedang berjalan.
        if (loggedVersion(raw) != versionCode) {
            clear(context)
            return null
        }
        return raw
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }
}

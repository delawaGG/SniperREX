package com.sniperrex.app

import android.content.Context
import java.io.File

/**
 * Menyimpan stacktrace crash terakhir ke file internal, supaya bisa dibaca
 * di layar berikutnya tanpa perlu logcat / adb.
 */
object CrashGuard {

    private const val FILE_NAME = "last_crash.txt"

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val header = "thread=${thread.name}\ndevice=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} " +
                    "sdk=${android.os.Build.VERSION.SDK_INT}\n\n"
                file(context).writeText(header + error.stackTraceToString())
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? =
        runCatching { file(context).takeIf { it.exists() }?.readText() }.getOrNull()
            ?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }
}

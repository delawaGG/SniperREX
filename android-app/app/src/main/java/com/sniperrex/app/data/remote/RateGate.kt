package com.sniperrex.app.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay

/**
 * Pembatas laju global untuk Twelve Data.
 *
 * Plan FREE hanya memberi ~8 kredit/menit. Dashboard lama menembak
 * 1 chart + 4 radar + sweep sekaligus, jadi sebagian besar request
 * dijawab 429 dan `fetch()` mengembalikan null -> chart "kadang hilang".
 *
 * Semua request lewat gerbang ini supaya serial dan berjarak.
 */
object RateGate {

    /** Jeda antar request. 8 detik = 7,5 kredit/menit, aman di bawah 8. */
    private const val MIN_INTERVAL_MS = 8_000L

    private val mutex = Mutex()
    private var lastAt = 0L

    /** Menunggu sampai aman untuk mengirim request berikutnya. */
    suspend fun acquire() = mutex.withLock {
        val now = System.currentTimeMillis()
        val wait = MIN_INTERVAL_MS - (now - lastAt)
        if (wait > 0) delay(wait)
        lastAt = System.currentTimeMillis()
    }

    /** Dipakai saat menerima 429: mundur lebih jauh sebelum mencoba lagi. */
    suspend fun penalise(millis: Long = 20_000L) = mutex.withLock {
        lastAt = System.currentTimeMillis() + millis - MIN_INTERVAL_MS
    }
}

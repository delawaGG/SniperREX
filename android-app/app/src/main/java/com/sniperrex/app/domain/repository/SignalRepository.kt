package com.sniperrex.app.domain.repository

import com.sniperrex.app.domain.model.AiVerdict
import com.sniperrex.app.domain.model.ScanParams
import com.sniperrex.app.domain.model.ScanResult
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.SignalStatus
import kotlinx.coroutines.flow.Flow

interface SignalRepository {
    suspend fun scan(params: ScanParams): ScanResult
    fun observeSignals(): Flow<List<Signal>>
    fun observeSignal(id: String): Flow<Signal?>
    suspend fun updateStatus(id: String, status: SignalStatus)
    suspend fun analyze(signalId: String): AiVerdict
    suspend fun tierTimeframes(): List<String>
    suspend fun testTwelve(): Pair<Boolean, String>
    suspend fun testAi(): Pair<Boolean, String>
}

package com.sniperrex.app.data.local

import kotlinx.serialization.Serializable

@Serializable
data class AiVerdictRecord(
    val score: Int,
    val verdict: String,
    val reasoning: String,
    val risks: List<String> = emptyList(),
    val finalDecision: String? = null,
    val finalReasons: List<String> = emptyList(),
)

@Serializable
data class ContextRecord(
    val trend: String,
    val rsi: Double,
    val atr: Double,
    val session: String,
)

@Serializable
data class ConfluenceRecord(val total: Int, val threshold: Int)

package com.sniperrex.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey val id: String,
    val pair: String,
    val timeframe: String,
    val direction: String,
    val coreStrategy: String,
    val structureEvent: String,
    val confirmations: String,   // JSON array
    val entry: Double,
    val stopLoss: Double,
    val takeProfits: String,     // JSON array
    val riskReward: Double,
    val quantScore: Int,
    val status: String,
    val confluenceJson: String,
    val contextJson: String,
    val aiVerdictJson: String?,
    val createdAt: Long,
    val syncedAt: Long,
)

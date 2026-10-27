package com.sniperrex.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanRequestDto(
    val pair: String,
    val timeframe: String,
    @SerialName("core_strategy") val coreStrategy: String,
    val confirmations: List<String>,
)

@Serializable
data class ConfluenceDto(
    @SerialName("core_passed") val corePassed: Boolean,
    @SerialName("rsi_passed") val rsiPassed: Boolean,
    @SerialName("atr_passed") val atrPassed: Boolean,
    @SerialName("volume_passed") val volumePassed: Boolean,
    @SerialName("ob_passed") val obPassed: Boolean,
    @SerialName("total_confluence") val totalConfluence: Int,
    val threshold: Int,
)

@Serializable
data class ContextDto(
    @SerialName("current_price") val currentPrice: Double,
    val atr: Double,
    val rsi: Double,
    val trend: String,
    val session: String,
)

@Serializable
data class SignalDto(
    val id: String,
    val pair: String,
    val timeframe: String,
    val direction: String,
    @SerialName("core_strategy") val coreStrategy: String,
    @SerialName("structure_event") val structureEvent: String = "NONE",
    val confirmations: List<String> = emptyList(),
    val entry: Double,
    @SerialName("stop_loss") val stopLoss: Double,
    @SerialName("take_profit") val takeProfit: List<Double>,
    @SerialName("risk_reward") val riskReward: Double,
    @SerialName("quant_score") val quantScore: Int,
    val confluence: ConfluenceDto,
    val context: ContextDto,
    @SerialName("created_at") val createdAt: String,
    val status: String,
)

@Serializable
data class SignalEnvelopeDto(
    val disclaimer: String,
    val signal: SignalDto? = null,
    val reason: String? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class SignalListEnvelopeDto(
    val disclaimer: String,
    val signals: List<SignalDto> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class StatusUpdateDto(val status: String)

@Serializable
data class AiAnalyzeDto(@SerialName("signal_id") val signalId: String)

@Serializable
data class AiChatDto(
    @SerialName("signal_id") val signalId: String,
    val message: String,
)

@Serializable
data class AiChatReplyDto(
    @SerialName("signal_id") val signalId: String,
    val reply: String,
)

@Serializable
data class AiVerdictDto(
    @SerialName("signal_id") val signalId: String,
    @SerialName("ai_score") val aiScore: Int,
    val verdict: String,
    val reasoning: String,
    val risks: List<String> = emptyList(),
    @SerialName("model_used") val modelUsed: String = "",
    @SerialName("final_decision") val finalDecision: String? = null,
    @SerialName("final_reasons") val finalReasons: List<String> = emptyList(),
)

@Serializable
data class TierInfoDto(
    val tier: String,
    val timeframes: List<String>,
    @SerialName("htf_proxy") val htfProxy: String,
)

@Serializable
data class ProviderHealthDto(
    val ok: Boolean,
    val detail: String,
    val extra: Map<String, String> = emptyMap(),
)

package com.sniperrex.app.data.remote

import com.sniperrex.app.domain.model.AiVerdict
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.Verdict
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI second-opinion langsung dari perangkat (OpenAI-compatible).
 * Constraint prompt: dilarang menyarankan lot size / leverage.
 */
@Singleton
class AiGateway @Inject constructor(private val json: Json) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are a professional forex/gold trading analyst reviewing a deterministic signal.
        Rules: do NOT recommend position sizing, lot size, leverage, or account risk.
        Do not invent levels; use only entry, stop_loss, take_profit given.
        Answer "reasoning" and "risks" in Bahasa Indonesia.
        Output ONLY JSON: {"ai_score": int 0-100, "verdict": "TAKE"|"WAIT"|"SKIP",
        "reasoning": "2-3 sentences", "risks": ["...", "..."]}
    """.trimIndent()

    suspend fun analyze(signal: Signal, apiKey: String, baseUrl: String, model: String): AiVerdict {
        val payload = buildJsonObject {
            put("model", model)
            put("temperature", 0.2)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", signalContext(signal))
                }
            }
        }
        val content = post("/chat/completions", payload.toString(), apiKey, baseUrl)
        return parseVerdict(content)
    }

    suspend fun ping(apiKey: String, baseUrl: String): String {
        val response = get("/models", apiKey, baseUrl)
        val parsed = runCatching { json.parseToJsonElement(response).jsonObject["data"]!!.jsonArray.size }.getOrDefault(0)
        return "endpoint AI terjangkau ($parsed model)"
    }

    private fun signalContext(signal: Signal): String = buildString {
        appendLine("Review this signal and return the JSON verdict.")
        appendLine("pair=${signal.pair} timeframe=${signal.timeframe} direction=${signal.direction.name}")
        appendLine("strategy=${signal.coreStrategy.name} event=${signal.structureEvent}")
        appendLine("entry=${signal.entry} stop_loss=${signal.stopLoss} take_profit=${signal.takeProfits}")
        appendLine("rr_tp1=${signal.riskReward} quant_score=${signal.quantScore}")
        appendLine("confluence=${signal.totalConfluence}/${signal.threshold}")
        appendLine("trend=${signal.trend} rsi=${signal.rsi} atr=${signal.atr} session=${signal.session}")
    }

    private fun parseVerdict(raw: String): AiVerdict {
        val candidate = Regex("\\{[\\s\\S]*\\}").find(raw)?.value ?: raw
        val obj: JsonObject? = runCatching { json.parseToJsonElement(candidate).jsonObject }.getOrNull()
        if (obj == null) {
            return AiVerdict(0, Verdict.WAIT, "AI parse error — verdict tidak dipakai",
                listOf("Respons AI tidak valid; andalkan skor kuantitatif."))
        }
        val score = obj["ai_score"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.toInt() ?: 0
        val verdict = runCatching { Verdict.valueOf(obj["verdict"]!!.jsonPrimitive.content.uppercase()) }
            .getOrDefault(Verdict.WAIT)
        val risks = runCatching {
            obj["risks"]!!.jsonArray.map { it.jsonPrimitive.content }
        }.getOrDefault(emptyList())
        return AiVerdict(
            aiScore = score.coerceIn(0, 100),
            verdict = verdict,
            reasoning = obj["reasoning"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            risks = risks,
        )
    }

    private fun post(path: String, body: String, apiKey: String, baseUrl: String): String {
        val url = baseUrl.trimEnd('/') + path
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("AI HTTP ${response.code}: ${text.take(160)}")
            val obj = json.parseToJsonElement(text).jsonObject
            return obj["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!
                .jsonPrimitive.content
        }
    }

    private fun get(path: String, apiKey: String, baseUrl: String): String {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("AI HTTP ${response.code}: ${text.take(160)}")
            return text
        }
    }
}

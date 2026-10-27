package com.sniperrex.app.data.repository

import com.sniperrex.app.data.local.AiVerdictRecord
import com.sniperrex.app.data.local.ConfluenceRecord
import com.sniperrex.app.data.local.ContextRecord
import com.sniperrex.app.data.local.SettingsStore
import com.sniperrex.app.data.local.SignalDao
import com.sniperrex.app.data.local.SignalEntity
import com.sniperrex.app.data.remote.AiGateway
import com.sniperrex.app.data.worker.ScanConfig
import com.sniperrex.app.data.remote.TwelveDataApi
import com.sniperrex.app.domain.engine.AnalysisEngine
import com.sniperrex.app.domain.engine.Candle
import com.sniperrex.app.domain.engine.Indicators
import com.sniperrex.app.domain.engine.Intervals
import com.sniperrex.app.domain.model.AiVerdict
import com.sniperrex.app.domain.model.CoreStrategy
import com.sniperrex.app.domain.model.Direction
import com.sniperrex.app.domain.model.MarketPulse
import com.sniperrex.app.domain.model.PulseState
import com.sniperrex.app.domain.model.PairInfo
import com.sniperrex.app.domain.model.Pairs
import com.sniperrex.app.domain.model.ScanParams
import com.sniperrex.app.domain.model.ScanResult
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.SignalStatus
import com.sniperrex.app.domain.model.Verdict
import com.sniperrex.app.domain.repository.SignalRepository
import com.sniperrex.app.domain.repository.SweepReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sepenuhnya di perangkat: Twelve Data dipanggil langsung memakai key user,
 * sinyal dihitung AnalysisEngine, disimpan di Room. Tidak butuh server.
 */
@Singleton
class LocalSignalRepository @Inject constructor(
    private val api: TwelveDataApi,
    private val dao: SignalDao,
    private val settings: SettingsStore,
    private val engine: AnalysisEngine,
    private val ai: AiGateway,
    private val json: Json,
) : SignalRepository {

    override suspend fun scan(params: ScanParams): ScanResult = withContext(Dispatchers.IO) {
        val creds = settings.current()
        if (creds.twelveKey.isBlank()) {
            return@withContext ScanResult(null, "Isi Twelve Data API key dulu di Settings", emptyList())
        }
        val info = Pairs.of(params.pair)
            ?: return@withContext ScanResult(null, "pair tidak dikenal: ${params.pair}", emptyList())

        val candles = fetch(info, params.timeframe, 300)
            ?: return@withContext ScanResult(null, "gagal menarik data ${info.symbol} ${params.timeframe}", emptyList())
        val htf = fetch(info, "M45", 200)

        val outcome = engine.analyse(
            info = info,
            timeframe = params.timeframe,
            strategy = params.coreStrategy,
            confirmations = params.confirmations,
            candles = candles,
            htfCandles = htf,
            htfTimeframe = "M45",
        )
        outcome.signal?.let { dao.upsert(toEntity(it)) }
        outcome
    }

    private suspend fun fetch(info: PairInfo, timeframe: String, outputSize: Int): List<Candle>? {
        val creds = settings.current()
        val interval = Intervals.MAP[timeframe] ?: return null
        val response = runCatching {
            api.timeSeries(
                symbol = info.tdSymbol,
                interval = interval,
                outputSize = outputSize,
                apiKey = creds.twelveKey,
            )
        }.getOrNull() ?: return null
        if (response.status == "error" || response.values.isEmpty()) return null
        return response.values.mapNotNull { bar ->
            val open = bar.open.toDoubleOrNull() ?: return@mapNotNull null
            val high = bar.high.toDoubleOrNull() ?: return@mapNotNull null
            val low = bar.low.toDoubleOrNull() ?: return@mapNotNull null
            val close = bar.close.toDoubleOrNull() ?: return@mapNotNull null
            Candle(
                timeMs = parseTime(bar.datetime),
                open = open, high = high, low = low, close = close,
                volume = bar.volume?.toDoubleOrNull() ?: 0.0,
            )
        }.sortedBy { it.timeMs }
    }

    private fun parseTime(raw: String): Long = runCatching {
        val normalised = if (raw.length == 10) "${raw}T00:00:00Z" else raw.replace(" ", "T") + "Z"
        Instant.parse(normalised).toEpochMilli()
    }.getOrDefault(0L)

    override fun observeSignals(): Flow<List<Signal>> =
        dao.observeRecent().map { list -> list.map(::toDomain) }

    override fun observeSignal(id: String): Flow<Signal?> =
        dao.observeById(id).map { it?.let(::toDomain) }

    override suspend fun updateStatus(id: String, status: SignalStatus) {
        dao.updateStatus(id, status.name)
    }

    override suspend fun analyze(signalId: String): AiVerdict = withContext(Dispatchers.IO) {
        val entity = dao.findById(signalId) ?: error("signal tidak ditemukan")
        val signal = toDomain(entity)
        val creds = settings.current()
        if (creds.aiKey.isBlank()) error("Isi AI API key dulu di Settings")
        val baseUrl = creds.aiBaseUrl.ifBlank { DEFAULT_AI_BASE }
        val model = creds.aiModel.ifBlank { DEFAULT_AI_MODEL }

        val raw = ai.analyze(signal, creds.aiKey, baseUrl, model)
        val final = engine.applyVerdict(signal, raw)
        dao.updateVerdict(signalId, json.encodeToString(AiVerdictRecord.serializer(), toRecord(final)))
        final
    }

    override suspend fun tierTimeframes(): List<String> = listOf("M15", "M30", "M45")

    /**
     * Uji key nyata. `api_usage` dipakai lebih dulu (0 kredit), lalu jatuh ke
     * satu request time_series kecil kalau endpoint itu tidak tersedia pada plan
     * tertentu. Key selalu dibaca dari DataStore, jadi pemanggil wajib menyimpan
     * key lebih dulu (dilakukan SettingsViewModel sebelum memanggil ini).
     */
    override suspend fun testTwelve(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val key = settings.current().twelveKey.trim()
        if (key.isBlank()) return@withContext false to "API key kosong"

        val usageAttempt = runCatching { api.apiUsage(key) }
        val usage = usageAttempt.getOrNull()
        if (usage != null) {
            if (usage.status == "error" || usage.code != null) {
                return@withContext false to (usage.message ?: "key ditolak (kode ${usage.code})")
            }
            val plan = usage.planCategory.orEmpty().ifBlank { "free" }
            val daily = usage.dailyUsage ?: 0
            val limit = usage.planDailyLimit ?: 0
            val suffix = if (limit > 0) " · $daily/$limit kredit" else ""
            return@withContext true to "terhubung · plan $plan$suffix"
        }

        // Fallback: validasi key lewat satu bar XAU/USD.
        val probe = runCatching {
            api.timeSeries("XAU/USD", "15min", 1, apiKey = key)
        }
        val series = probe.getOrNull()
            ?: return@withContext false to (probe.exceptionOrNull()?.message ?: "tidak bisa menghubungi Twelve Data")
        if (series.status == "error" || series.values.isEmpty()) {
            return@withContext false to (series.message ?: "key ditolak atau data kosong")
        }
        true to "terhubung · data XAU/USD diterima"
    }

    override suspend fun testAi(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val creds = settings.current()
        if (creds.aiKey.isBlank()) return@withContext false to "AI key kosong"
        val baseUrl = creds.aiBaseUrl.ifBlank { DEFAULT_AI_BASE }
        runCatching { ai.ping(creds.aiKey, baseUrl) }
            .fold({ true to it }, { false to (it.message ?: "gagal") })
    }



    // ---------------- sweep multi-strategi ----------------

    /**
     * Satu fetch per (pair, timeframe) lalu jalankan semua strategi di memori.
     * Hasil dibatasi: maksimum 1 signal per pair+timeframe+strategi per bar
     * (dedupe lewat DAO) supaya journal tidak banjir duplikat tiap 15 menit.
     */
    override suspend fun sweep(): SweepReport = withContext(Dispatchers.IO) {
        val key = settings.current().twelveKey
        if (key.isBlank()) return@withContext SweepReport(0, emptyList(), 0, "API key belum diisi")

        val pairs = ScanConfig.ALL_PAIRS
        val timeframes = listOf("M15", "H1")
        val strategies = listOf(
            CoreStrategy.FVG,
            CoreStrategy.ORDER_BLOCK_BOS,
            CoreStrategy.TREND_STRUCTURE,
            CoreStrategy.EMA_CROSS,
            CoreStrategy.MACD_CROSS,
        )
        val confirmations = listOf("RSI", "ATR", "OB")
        val intervalMs = 15 * 60_000L

        val found = mutableListOf<Signal>()
        var scanned = 0
        var skipped = 0

        for (pair in pairs) {
            val info = Pairs.of(pair) ?: continue
            for (tf in timeframes) {
                val candles = fetch(info, tf, 300)
                if (candles == null || candles.size < 60) {
                    skipped++
                    continue
                }
                val htf = fetch(info, "M45", 200)
                for (strategy in strategies) {
                    scanned++
                    val outcome = runCatching {
                        engine.analyse(
                            info = info,
                            timeframe = tf,
                            strategy = strategy,
                            confirmations = confirmations,
                            candles = candles,
                            htfCandles = htf,
                            htfTimeframe = "M45",
                        )
                    }.getOrNull()
                    val signal = outcome?.signal ?: continue

                    // Dedupe: jangan simpan setup sama berulang dalam satu bar.
                    val since = System.currentTimeMillis() - intervalMs
                    val duplicates = dao.countRecent(pair, tf, strategy.name, since)
                    if (duplicates > 0) {
                        skipped++
                        continue
                    }
                    dao.upsert(toEntity(signal))
                    found += signal
                }
            }
        }

        val message = when {
            found.isNotEmpty() -> "${found.size} setup baru dari $scanned evaluasi"
            scanned == 0 -> "data tidak tersedia — cek key atau kuota"
            else -> "$scanned evaluasi, tidak ada setup lolos gate"
        }
        SweepReport(scanned = scanned, found = found, skipped = skipped, message = message)
    }

    // ---------------- chart & radar ----------------

    override suspend fun candles(pair: String, timeframe: String, limit: Int): List<Candle> =
        withContext(Dispatchers.IO) {
            val info = Pairs.of(pair) ?: return@withContext emptyList()
            if (settings.current().twelveKey.isBlank()) return@withContext emptyList()
            fetch(info, timeframe, limit).orEmpty()
        }

    override suspend fun pulses(pairs: List<String>, timeframe: String): List<MarketPulse> =
        withContext(Dispatchers.IO) {
            val key = settings.current().twelveKey
            if (key.isBlank()) return@withContext emptyList()
            pairs.mapNotNull { symbol ->
                val info = Pairs.of(symbol) ?: return@mapNotNull null
                val series = fetch(info, timeframe, 120) ?: return@mapNotNull null
                if (series.size < 60) return@mapNotNull null
                val closes = DoubleArray(series.size) { series[it].close }
                val rsi = Indicators.rsi(closes).lastOrNull() ?: 50.0
                val atr = Indicators.atr(series).lastOrNull() ?: 0.0
                val emaFast = Indicators.ema(closes, 21).lastOrNull() ?: closes.last()
                val emaSlow = Indicators.ema(closes, 55).lastOrNull() ?: closes.last()
                val last = series.last().close
                val prev = series[maxOf(0, series.size - 5)].close
                val changePct = if (prev > 0) (last - prev) / prev * 100.0 else 0.0
                val atrPct = if (last > 0) atr / last * 100.0 else 0.0
                val bias = if (emaFast > emaSlow) "BULLISH" else "BEARISH"
                val state = when {
                    rsi >= 72 || rsi <= 28 -> PulseState.EXHAUSTED
                    atrPct < 0.05 -> PulseState.COMPRESSED
                    bias == "BULLISH" -> PulseState.TRENDING_UP
                    else -> PulseState.TRENDING_DOWN
                }
                MarketPulse(
                    pair = info.symbol,
                    price = last,
                    changePct = changePct,
                    rsi = rsi,
                    atr = atr,
                    bias = bias,
                    atrPct = atrPct,
                    state = state,
                )
            }
        }

    // ---------------- mapping ----------------
    private fun toEntity(signal: Signal) = SignalEntity(
        id = signal.id,
        pair = signal.pair,
        timeframe = signal.timeframe,
        direction = signal.direction.name,
        coreStrategy = signal.coreStrategy.name,
        structureEvent = signal.structureEvent,
        confirmations = json.encodeToString(ListSerializer(String.serializer()), signal.confirmations),
        entry = signal.entry,
        stopLoss = signal.stopLoss,
        takeProfits = json.encodeToString(ListSerializer(Double.serializer()), signal.takeProfits),
        riskReward = signal.riskReward,
        quantScore = signal.quantScore,
        status = signal.status.name,
        confluenceJson = json.encodeToString(
            ConfluenceRecord.serializer(),
            ConfluenceRecord(signal.totalConfluence, signal.threshold),
        ),
        contextJson = json.encodeToString(
            ContextRecord.serializer(),
            ContextRecord(signal.trend, signal.rsi, signal.atr, signal.session),
        ),
        aiVerdictJson = signal.aiVerdict?.let {
            json.encodeToString(AiVerdictRecord.serializer(), toRecord(it))
        },
        createdAt = signal.createdAtEpochMs,
        syncedAt = System.currentTimeMillis(),
    )

    private fun toDomain(entity: SignalEntity): Signal {
        val context = runCatching {
            json.decodeFromString(ContextRecord.serializer(), entity.contextJson)
        }.getOrNull()
        val confluence = runCatching {
            json.decodeFromString(ConfluenceRecord.serializer(), entity.confluenceJson)
        }.getOrNull()
        val verdict = entity.aiVerdictJson?.let {
            runCatching { json.decodeFromString(AiVerdictRecord.serializer(), it) }.getOrNull()
        }
        return Signal(
            id = entity.id,
            pair = entity.pair,
            timeframe = entity.timeframe,
            direction = runCatching { Direction.valueOf(entity.direction) }.getOrDefault(Direction.LONG),
            coreStrategy = runCatching { CoreStrategy.valueOf(entity.coreStrategy) }.getOrDefault(CoreStrategy.FVG),
            structureEvent = entity.structureEvent,
            confirmations = runCatching {
                json.decodeFromString(ListSerializer(String.serializer()), entity.confirmations)
            }.getOrDefault(emptyList()),
            entry = entity.entry,
            stopLoss = entity.stopLoss,
            takeProfits = runCatching {
                json.decodeFromString(ListSerializer(Double.serializer()), entity.takeProfits)
            }.getOrDefault(emptyList()),
            riskReward = entity.riskReward,
            quantScore = entity.quantScore,
            status = runCatching { SignalStatus.valueOf(entity.status) }.getOrDefault(SignalStatus.WAIT),
            totalConfluence = confluence?.total ?: 0,
            threshold = confluence?.threshold ?: 0,
            trend = context?.trend ?: "-",
            rsi = context?.rsi ?: 0.0,
            atr = context?.atr ?: 0.0,
            session = context?.session ?: "-",
            createdAtEpochMs = entity.createdAt,
            aiVerdict = verdict?.let {
                AiVerdict(
                    aiScore = it.score,
                    verdict = runCatching { Verdict.valueOf(it.verdict) }.getOrDefault(Verdict.WAIT),
                    reasoning = it.reasoning,
                    risks = it.risks,
                    finalDecision = it.finalDecision?.let { d -> runCatching { Verdict.valueOf(d) }.getOrNull() },
                    finalReasons = it.finalReasons,
                )
            },
        )
    }

    private fun toRecord(verdict: AiVerdict) = AiVerdictRecord(
        score = verdict.aiScore,
        verdict = verdict.verdict.name,
        reasoning = verdict.reasoning,
        risks = verdict.risks,
        finalDecision = verdict.finalDecision?.name,
        finalReasons = verdict.finalReasons,
    )

    private companion object {
        const val DEFAULT_AI_BASE = "https://api.groq.com/openai/v1"
        const val DEFAULT_AI_MODEL = "llama-3.3-70b-versatile"
    }
}

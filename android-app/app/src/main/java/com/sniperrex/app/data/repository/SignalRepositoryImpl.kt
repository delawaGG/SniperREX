package com.sniperrex.app.data.repository

import com.sniperrex.app.data.local.SettingsStore
import com.sniperrex.app.data.local.SignalDao
import com.sniperrex.app.data.local.SignalEntity
import com.sniperrex.app.data.remote.AiAnalyzeDto
import com.sniperrex.app.data.remote.ScanRequestDto
import com.sniperrex.app.data.remote.SignalDto
import com.sniperrex.app.data.remote.SniperRexApi
import com.sniperrex.app.data.remote.StatusUpdateDto
import com.sniperrex.app.di.ApiKeyProvider
import com.sniperrex.app.di.BackendUrlProvider
import com.sniperrex.app.domain.model.AiVerdict
import com.sniperrex.app.domain.model.CoreStrategy
import com.sniperrex.app.domain.model.Direction
import com.sniperrex.app.domain.model.ScanParams
import com.sniperrex.app.domain.model.ScanResult
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.domain.model.SignalStatus
import com.sniperrex.app.domain.model.Verdict
import com.sniperrex.app.domain.repository.SignalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.sniperrex.app.data.remote.AiVerdictDto
import com.sniperrex.app.data.remote.ConfluenceDto
import com.sniperrex.app.data.remote.ContextDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalRepositoryImpl @Inject constructor(
    private val api: SniperRexApi,
    private val dao: SignalDao,
    private val settings: SettingsStore,
    private val keyProvider: ApiKeyProvider,
    private val backendUrl: BackendUrlProvider,
    private val json: Json,
) : SignalRepository {

    /** Sinkronkan kredensial user ke interceptor sebelum request. */
    private suspend fun syncCredentials() {
        val creds = settings.current()
        backendUrl.set(creds.backendUrl)
        keyProvider.setTwelveKey(creds.twelveKey)
        keyProvider.setAi(creds.aiKey, creds.aiBaseUrl, creds.aiModel)
    }

    override suspend fun scan(params: ScanParams): ScanResult {
        syncCredentials()
        val envelope = api.scan(
            ScanRequestDto(
                pair = params.pair,
                timeframe = params.timeframe,
                coreStrategy = params.coreStrategy.name,
                confirmations = params.confirmations,
            )
        )
        val dto = envelope.signal ?: return ScanResult(null, envelope.reason, envelope.warnings)
        val domain = dto.toDomain()
        dao.upsert(dto.toEntity())
        return ScanResult(domain, null, envelope.warnings)
    }

    override fun observeSignals(): Flow<List<Signal>> =
        dao.observeRecent().map { list -> list.map { it.toDomain() } }

    override fun observeSignal(id: String): Flow<Signal?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun updateStatus(id: String, status: SignalStatus) {
        syncCredentials()
        api.updateStatus(id, StatusUpdateDto(status.name))
        dao.updateStatus(id, status.name)
    }

    override suspend fun analyze(signalId: String): AiVerdict {
        syncCredentials()
        val dto = api.analyze(AiAnalyzeDto(signalId))
        dao.updateVerdict(signalId, json.encodeToString(AiVerdictDto.serializer(), dto))
        return dto.toDomain()
    }

    override suspend fun tierTimeframes(): List<String> {
        syncCredentials()
        return api.tierInfo().timeframes
    }

    override suspend fun testTwelve(): Pair<Boolean, String> {
        syncCredentials()
        val r = api.healthTwelve()
        return r.ok to r.detail
    }

    override suspend fun testAi(): Pair<Boolean, String> {
        syncCredentials()
        val r = api.healthAi()
        return r.ok to r.detail
    }

    private fun SignalDto.toDomain(): Signal = Signal(
        id = id,
        pair = pair,
        timeframe = timeframe,
        direction = Direction.valueOf(direction),
        coreStrategy = CoreStrategy.valueOf(coreStrategy),
        structureEvent = structureEvent,
        confirmations = confirmations,
        entry = entry,
        stopLoss = stopLoss,
        takeProfits = takeProfit,
        riskReward = riskReward,
        quantScore = quantScore,
        status = SignalStatus.valueOf(status),
        totalConfluence = confluence.totalConfluence,
        threshold = confluence.threshold,
        trend = context.trend,
        rsi = context.rsi,
        atr = context.atr,
        session = context.session,
        createdAtEpochMs = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0L),
    )

    private fun SignalDto.toEntity(): SignalEntity = SignalEntity(
        id = id,
        pair = pair,
        timeframe = timeframe,
        direction = direction,
        coreStrategy = coreStrategy,
        structureEvent = structureEvent,
        confirmations = json.encodeToString(ListSerializer(String.serializer()), confirmations),
        entry = entry,
        stopLoss = stopLoss,
        takeProfits = json.encodeToString(ListSerializer(Double.serializer()), takeProfit),
        riskReward = riskReward,
        quantScore = quantScore,
        status = status,
        confluenceJson = json.encodeToString(ConfluenceDto.serializer(), confluence),
        contextJson = json.encodeToString(ContextDto.serializer(), context),
        aiVerdictJson = null,
        createdAt = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0L),
        syncedAt = System.currentTimeMillis(),
    )

    private fun SignalEntity.toDomain(): Signal {
        val confluence = runCatching { json.decodeFromString(ConfluenceDto.serializer(), confluenceJson) }.getOrNull()
        val context = runCatching { json.decodeFromString(ContextDto.serializer(), contextJson) }.getOrNull()
        return Signal(
            id = id,
            pair = pair,
            timeframe = timeframe,
            direction = Direction.valueOf(direction),
            coreStrategy = CoreStrategy.valueOf(coreStrategy),
            structureEvent = structureEvent,
            confirmations = runCatching { json.decodeFromString(ListSerializer(String.serializer()), confirmations) }.getOrDefault(emptyList()),
            entry = entry,
            stopLoss = stopLoss,
            takeProfits = runCatching { json.decodeFromString(ListSerializer(Double.serializer()), takeProfits) }.getOrDefault(emptyList()),
            riskReward = riskReward,
            quantScore = quantScore,
            status = runCatching { SignalStatus.valueOf(status) }.getOrDefault(SignalStatus.WAIT),
            totalConfluence = confluence?.totalConfluence ?: 0,
            threshold = confluence?.threshold ?: 0,
            trend = context?.trend ?: "-",
            rsi = context?.rsi ?: 0.0,
            atr = context?.atr ?: 0.0,
            session = context?.session ?: "-",
            createdAtEpochMs = createdAt,
            aiVerdict = aiVerdictJson?.let { raw ->
                runCatching {
                    val dto = json.decodeFromString(AiVerdictDto.serializer(), raw)
                    dto.toDomain()
                }.getOrNull()
            },
        )
    }

    private fun AiVerdictDto.toDomain(): AiVerdict = AiVerdict(
        aiScore = aiScore,
        verdict = runCatching { Verdict.valueOf(verdict) }.getOrDefault(Verdict.WAIT),
        reasoning = reasoning,
        risks = risks,
        finalDecision = finalDecision?.let { runCatching { Verdict.valueOf(it) }.getOrNull() },
    )
}

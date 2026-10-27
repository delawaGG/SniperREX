package com.sniperrex.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Signal-only API. Tidak ada endpoint order/eksekusi — sesuai spec §5. */
interface SniperRexApi {

    @POST("api/scan")
    suspend fun scan(@Body body: ScanRequestDto): SignalEnvelopeDto

    @GET("api/signals")
    suspend fun signals(
        @Query("pair") pair: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
    ): SignalListEnvelopeDto

    @GET("api/signals/{id}")
    suspend fun signal(@Path("id") id: String): SignalEnvelopeDto

    @PUT("api/signals/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body body: StatusUpdateDto): SignalEnvelopeDto

    @POST("api/ai/analyze")
    suspend fun analyze(@Body body: AiAnalyzeDto): AiVerdictDto

    @POST("api/ai/chat")
    suspend fun chat(@Body body: AiChatDto): AiChatReplyDto

    @GET("api/tier-info")
    suspend fun tierInfo(): TierInfoDto

    @GET("api/health/twelve")
    suspend fun healthTwelve(): ProviderHealthDto

    @GET("api/health/ai")
    suspend fun healthAi(): ProviderHealthDto
}

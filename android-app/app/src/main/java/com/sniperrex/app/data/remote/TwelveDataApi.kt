package com.sniperrex.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class TwelveBar(
    val datetime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String? = null,
)

@Serializable
data class TwelveSeriesResponse(
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null,
    val values: List<TwelveBar> = emptyList(),
)

@Serializable
data class TwelveUsageResponse(
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null,
    @SerialName("plan_category") val planCategory: String? = null,
    @SerialName("plan_limit") val planLimit: Int? = null,
    @SerialName("plan_daily_limit") val planDailyLimit: Int? = null,
    @SerialName("daily_usage") val dailyUsage: Int? = null,
    @SerialName("current_usage") val currentUsage: Int? = null,
)

/** Twelve Data dipanggil langsung dari perangkat memakai key masing-masing user. */
interface TwelveDataApi {

    @GET("time_series")
    suspend fun timeSeries(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("outputsize") outputSize: Int,
        @Query("order") order: String = "ASC",
        @Query("timezone") timezone: String = "UTC",
        @Query("apikey") apiKey: String,
    ): TwelveSeriesResponse

    @GET("api_usage")
    suspend fun apiUsage(@Query("apikey") apiKey: String): TwelveUsageResponse
}

package tv.caffeine.app.api

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import tv.caffeine.app.di.ApiConfig

interface EventsService {
    @POST(ApiConfig.EVENTS_SERVICE_STATS_PATH)
    fun sendStats(@Body cumulativeCounters: CumulativeCounters): Deferred<Response<Any>>

    @POST(ApiConfig.EVENTS_SERVICE_STATS_PATH)
    fun sendCounters(@Body counters: Counters): Deferred<Response<Any>>
}

sealed class StatsBody
class CumulativeCounters(val cumulativeCounters: List<StatsSnippet>) : StatsBody()
class StatsSnippet(val dimensions: StatsDimensions, val metricName: String, val value: Int)
class StatsDimensions(
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("sourceName") val sourceName: String,
    @SerializedName("stageId") val stageId: String
)

class Counters(val counters: List<StatsCounter>) : StatsBody()
class StatsCounter(val metricName: String, val value: Int)

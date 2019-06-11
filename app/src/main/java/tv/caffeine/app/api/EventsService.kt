package tv.caffeine.app.api

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.di.ApiConfig

interface EventsService {
    @POST("v1/stats")
    fun sendStats(@Body cumulativeCounters: CumulativeCounters): Deferred<Response<Any>>

    @POST("v1/stats")
    fun sendCounters(@Body counters: Counters): Deferred<Response<Any>>

    @POST("v1/events")
    fun sendEvent(@Body eventBody: EventBody): Deferred<Response<Any>>
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

sealed class EventBody(val eventType: String, val data: EventData, val eventSource: String = "android", val environment: String = ApiConfig.EVENTS_ENVIRONMENT)
class LobbyCardClickedEvent(data: LobbyClickedEventData) : EventBody("lobby_card_clicked", data)
class LobbyFollowClickedEvent(data: LobbyClickedEventData) : EventBody("lobby_follow_clicked", data)
class LobbyImpressionEvent(data: LobbyImpressionEventData) : EventBody("lobby_impression", data)

sealed class EventData
class LobbyClickedEventData(val payloadId: String, val caid: CAID?, val stageId: String, val clickedAt: Long) : EventData()

class LobbyImpressionEventData(
    val payloadId: String?,
    val caid: CAID?,
    val stageId: String,
    val featured: Boolean,
    val isLive: Boolean,
    val displayOrder: Int,
    val friendsWatching: List<String>,
    val category: String = "friends_activity" // TODO: Need to pull real category
) : EventData()
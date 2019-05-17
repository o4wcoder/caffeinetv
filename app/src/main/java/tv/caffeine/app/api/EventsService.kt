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

sealed class EventBody(eventSource: String = "android", environment: String = ApiConfig.EVENTS_ENVIRONMENT, eventType: String)
class LobbyCardClickedEvent(val data: LobbyClickedEventData) : EventBody(eventType = "lobby_card_clicked")
class LobbyFollowClickedEvent(val data: LobbyClickedEventData) : EventBody(eventType = "lobby_follow_clicked")
class LobbyClickedEventData(val pageLoadId: String, val caid: CAID, val stageId: String, val clickedAt: String)

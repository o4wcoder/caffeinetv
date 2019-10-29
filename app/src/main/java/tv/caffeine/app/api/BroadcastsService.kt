package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.di.ASSETS_BASE_URL

interface BroadcastsService {
    @GET("v1/broadcasts/{broadcastId}")
    fun broadcastDetails(@Path("broadcastId") broadcastId: String): Deferred<Response<BroadcastEnvelope>>

    @GET("v1/live-hostable-broadcasters")
    fun liveHostableBroadcasters(): Deferred<Response<BroadcasterList>>
}

interface ContentGuideService {
    @GET("v1/hostable")
    fun guide(): Deferred<Response<GuideList>>

    @GET("v1/featured")
    suspend fun featuredGuide(): FeaturedGuideList
}

class BroadcastEnvelope(val broadcast: Broadcast)

class GuideList(val listings: List<Guide>)

class Guide(val caid: CAID, val id: String, val title: String, val startTimestamp: Long, val endTimestamp: Long, var shouldShowTimestamp: Boolean)

class BroadcasterList(val broadcasters: List<Lobby.Broadcaster>)

class FeaturedGuideList(val listings: List<FeaturedGuideListing>)

class FeaturedGuideListing(val caid: CAID, val id: String, val category: String, val title: String, val startTimestamp: Long, val endTimestamp: Long, val description: String, private val detailImage: String?, val isUsOnly: Boolean) {
    val detailImageUrl get() = detailImage?.let { "$ASSETS_BASE_URL$it" }
}

package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaidRecord

interface BroadcastsService {
    @GET("v1/broadcasts/{broadcastId}")
    fun broadcastDetails(@Path("broadcastId") broadcastId: String): Deferred<Response<BroadcastEnvelope>>

    @GET("v1/broadcasts/{broadcastId}/friends-watching")
    fun friendsWatching(@Path("broadcastId") broadcastId: String): Deferred<Response<List<CaidRecord.FriendWatching>>>

    @GET("v1/guide")
    fun guide():  Deferred<Response<GuideList>>
}

class BroadcastEnvelope(val broadcast: Broadcast)

class GuideList(val listings: List<Guide>)

class Guide(val caid: CAID, val id: String, val title:String, val startTimestamp: Long, val endTimestamp: Long, var shouldShowTimestamp: Boolean)

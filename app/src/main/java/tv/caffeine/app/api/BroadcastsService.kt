package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CaidRecord

interface BroadcastsService {
    @GET("v1/broadcasts/{broadcastId}")
    fun broadcastDetails(@Path("broadcastId") broadcastId: String): Deferred<Response<BroadcastEnvelope>>

    @GET("v1/broadcasts/{broadcastId}/friends-watching")
    fun friendsWatching(@Path("broadcastId") broadcastId: String): Deferred<Response<List<CaidRecord.FriendWatching>>>
}

class BroadcastEnvelope(val broadcast: Broadcast)

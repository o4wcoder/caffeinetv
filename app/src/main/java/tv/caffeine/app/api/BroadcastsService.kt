package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Path
import tv.caffeine.app.api.model.Broadcast

interface BroadcastsService {
    @GET("v1/broadcasts/{broadcastId}")
    fun broadcastDetails(@Path("broadcastId") broadcastId: String): Deferred<BroadcastEnvelope>

    @GET("v1/broadcasts/{broadcastId}/friends-watching")
    fun friendsWatching(@Path("broadcastId") broadcastId: String): Deferred<List<FollowRecord>>
}

class BroadcastEnvelope(val broadcast: Broadcast)

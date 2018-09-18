package tv.caffeine.app.api

import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.GET
import retrofit2.http.Path

interface BroadcastsService {
    @GET("v1/broadcasts/{broadcastId}")
    fun broadcastDetails(@Path("broadcastId") broadcastId: String): Deferred<BroadcastEnvelope>
}

class BroadcastEnvelope(val broadcast: Api.Broadcast)

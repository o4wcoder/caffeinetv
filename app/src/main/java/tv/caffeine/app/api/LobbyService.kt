package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import tv.caffeine.app.api.model.Lobby

interface LobbyService {
    @GET("v3/lobby")
    fun loadLobby(): Deferred<Lobby>
}

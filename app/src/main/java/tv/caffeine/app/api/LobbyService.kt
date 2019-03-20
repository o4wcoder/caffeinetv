package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import tv.caffeine.app.api.model.Lobby

interface LobbyService {
    @GET("v4/lobby")
    fun loadLobby(): Deferred<Response<Lobby>>
}

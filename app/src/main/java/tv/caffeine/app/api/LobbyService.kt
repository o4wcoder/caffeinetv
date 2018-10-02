package tv.caffeine.app.api

import retrofit2.Call
import retrofit2.http.GET
import tv.caffeine.app.api.model.Lobby

interface LobbyService {
    @GET("v3/lobby")
    fun newLobby(): Call<Lobby.Result>
}

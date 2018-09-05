package tv.caffeine.app.api

import retrofit2.Call
import retrofit2.http.GET

interface LobbyService {
    @GET("v2/lobby")
    fun lobby(): Call<Api.LobbyResult>

    @GET("v3/lobby")
    fun newLobby(): Call<Api.v3.Lobby.Result>
}

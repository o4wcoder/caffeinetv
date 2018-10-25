package tv.caffeine.app.lobby

import com.google.gson.Gson
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import javax.inject.Inject

class LoadLobbyUseCase @Inject constructor(
        private val lobbyService: LobbyService,
        private val gson: Gson
) {
    suspend operator fun invoke(): CaffeineResult<Lobby> {
        val response = lobbyService.loadLobby().await()
        val lobby = response.body()
        val errorBody = response.errorBody()
        return when {
            response.isSuccessful && lobby != null -> CaffeineResult.Success(lobby)
            errorBody != null -> CaffeineResult.Error(gson.fromJson(errorBody.string(), ApiErrorResult::class.java))
            else -> CaffeineResult.Failure(Exception("Unknown state"))
        }
    }
}

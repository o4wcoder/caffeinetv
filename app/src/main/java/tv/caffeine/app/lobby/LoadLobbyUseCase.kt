package tv.caffeine.app.lobby

import com.google.gson.Gson
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class LoadLobbyUseCase @Inject constructor(
        private val lobbyService: LobbyService,
        private val gson: Gson
) {
    suspend operator fun invoke(): CaffeineResult<Lobby> =
            lobbyService.loadLobby().awaitAndParseErrors(gson)
}

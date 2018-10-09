package tv.caffeine.app.lobby

import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.Lobby
import javax.inject.Inject

class LoadLobbyUseCase @Inject constructor(private val lobbyService: LobbyService) {
    suspend operator fun invoke(): Lobby {
        return lobbyService.loadLobby().await()
    }
}

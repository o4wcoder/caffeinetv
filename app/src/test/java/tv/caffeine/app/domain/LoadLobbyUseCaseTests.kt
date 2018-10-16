package tv.caffeine.app.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.LoadLobbyUseCase

class LoadLobbyUseCaseTests {
    private val tags = mapOf<String, Lobby.Tag>()
    private val content = mapOf<String, Lobby.Content>()

    @Test
    fun lobbyLoadsFromLobbyService() {
        val mockLobbyService = object : LobbyService {
            override fun loadLobby() = async { Lobby(tags, content, Any(), arrayOf()) }
        }
        val useCase = LoadLobbyUseCase(mockLobbyService)
        val lobby = runBlocking { useCase() }
        Assert.assertTrue(lobby.content == content)
    }
}

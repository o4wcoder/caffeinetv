package tv.caffeine.app.domain

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.LoadLobbyUseCase

class LoadLobbyUseCaseTests {
    private val tags = mapOf<String, Lobby.Tag>()
    private val content = mapOf<String, Lobby.Content>()

    @Test
    fun lobbyLoadsFromLobbyService() {
        val mockLobbyResponse = mock<Deferred<Response<Lobby>>> {
            onBlocking { await() } doReturn Response.success(Lobby(tags, content, Any(), arrayOf()))
        }
        val fakeLobbyService = mock<LobbyService> {
            on { loadLobby() } doReturn mockLobbyResponse
        }
        val gson = Gson()
        val useCase = LoadLobbyUseCase(fakeLobbyService, gson)
        val result = runBlocking { useCase() }
        when (result) {
            is CaffeineResult.Success -> Assert.assertTrue(result.value.content == content)
            else -> Assert.fail("Was expecting lobby to load")
        }
    }
}

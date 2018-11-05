package tv.caffeine.app.domain

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.LoadLobbyUseCase

class LoadLobbyUseCaseTests {
    private val tags = mapOf<String, Lobby.Tag>()
    private val content = mapOf<String, Lobby.Content>()

    private lateinit var subject: LoadLobbyUseCase

    @Before
    fun setup() {
        val mockLobbyResponse = mock<Deferred<Response<Lobby>>> {
            onBlocking { await() } doReturn Response.success(Lobby(tags, content, Lobby.Header(), arrayOf()))
        }
        val fakeLobbyService = mock<LobbyService> {
            on { loadLobby() } doReturn mockLobbyResponse
        }
        val gson = Gson()
        subject = LoadLobbyUseCase(fakeLobbyService, gson)
    }

    @Test
    fun lobbyLoadsFromLobbyService() {
        val result = runBlocking { subject() }
        when (result) {
            is CaffeineResult.Success -> Assert.assertTrue(result.value.content === content)
            else -> Assert.fail("Was expecting lobby to load")
        }
    }
}

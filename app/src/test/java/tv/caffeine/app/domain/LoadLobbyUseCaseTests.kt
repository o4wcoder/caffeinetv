package tv.caffeine.app.domain

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.LoadLobbyUseCase

class LoadLobbyUseCaseTests {
    private val tags = mapOf<String, Lobby.Tag>()
    private val content = mapOf<String, Lobby.Content>()
    private val mockEventsService = mock<EventsService>()
    private val mockLobbyResponse = mock<Deferred<Response<Lobby>>> {
        onBlocking { await() } doReturn Response.success(Lobby(tags, content, Lobby.Header(), arrayOf()))
    }
    private val mockSuccessfulLobbyService = mock<LobbyService> {
        on { loadLobby() } doReturn mockLobbyResponse
    }
    private val mockFailedLobbyResponse = mock<Deferred<Response<Lobby>>> {
        onBlocking { await() } doReturn Response.error(404, ResponseBody.create(MediaType.parse("text/json"), "{}"))
    }
    private val mockFailingLobbyService = mock<LobbyService> {
        on { loadLobby() } doReturn mockFailedLobbyResponse
    }
    private val gson = Gson()

    @Test fun `lobby loads from lobby service`() {
        val subject = LoadLobbyUseCase(mockSuccessfulLobbyService, mockEventsService, gson)
        val result = runBlocking { subject() }
        when (result) {
            is CaffeineResult.Success -> Assert.assertTrue(result.value.content === content)
            else -> Assert.fail("Was expecting lobby to load")
        }
    }

    @Test fun `successful lobby request calls stats service`() {
        val subject = LoadLobbyUseCase(mockSuccessfulLobbyService, mockEventsService, gson)
        runBlocking { subject() }
        verify(mockEventsService).sendCounters(any())
    }

    @Test fun `failed lobby request does not call stats service`() {
        val subject = LoadLobbyUseCase(mockFailingLobbyService, mockEventsService, gson)
        runBlocking { subject() }
        verify(mockEventsService, never()).sendCounters(any())
    }

}

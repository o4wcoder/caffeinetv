package tv.caffeine.app.domain

import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.Counters
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.LoadLobbyUseCase

class LoadLobbyUseCaseTests {
    private val tags = mapOf<String, Lobby.Tag>()
    private val content = mapOf<String, Lobby.Content>()
    @MockK lateinit var mockEventsService: EventsService
    @MockK lateinit var mockLobbyResponse: Deferred<Response<Lobby>>
    @MockK lateinit var mockSuccessfulLobbyService: LobbyService
    @MockK lateinit var mockFailedLobbyResponse: Deferred<Response<Lobby>>
    @MockK lateinit var mockFailingLobbyService: LobbyService
    private val gson = Gson()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { mockLobbyResponse.await() } returns Response.success(Lobby(tags, content, Lobby.Header(), arrayOf()))
        every { mockSuccessfulLobbyService.loadLobby() } returns mockLobbyResponse
        coEvery { mockFailedLobbyResponse.await() } returns Response.error(404, ResponseBody.create(MediaType.parse("text/json"), "{}"))
        every { mockFailingLobbyService.loadLobby() } returns mockFailedLobbyResponse
        every { mockEventsService.sendCounters(any()) } returns mockk()
    }

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
        verify(exactly = 1) { mockEventsService.sendCounters(any()) }
    }

    @Test fun `successful lobby request increments lobby counter`() {
        val subject = LoadLobbyUseCase(mockSuccessfulLobbyService, mockEventsService, gson)
        val slot = slot<Counters>()
        every { mockEventsService.sendCounters(capture(slot)) } returns mockk()
        runBlocking { subject() }
        assertTrue("sendCounters must be called", slot.isCaptured)
        assertThat(slot.captured.counters.firstOrNull()?.metricName, equalTo("android.loadlobbywithsections.counter"))
    }

    @Test fun `failed lobby request does not call stats service`() {
        val subject = LoadLobbyUseCase(mockFailingLobbyService, mockEventsService, gson)
        runBlocking { subject() }
        verify(exactly = 0) { mockEventsService.sendCounters(any()) }
    }
}

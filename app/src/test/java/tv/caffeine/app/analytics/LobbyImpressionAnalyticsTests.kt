package tv.caffeine.app.analytics

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.Clock
import tv.caffeine.app.api.EventBody
import tv.caffeine.app.api.LobbyCardClickedEvent
import tv.caffeine.app.api.LobbyFollowClickedEvent
import tv.caffeine.app.api.LobbyImpressionEvent
import tv.caffeine.app.session.FollowManager

class LobbyImpressionAnalyticsTests {
    lateinit var subject: LobbyImpressionAnalytics
    @MockK(relaxed = true) lateinit var followManager: FollowManager
    @MockK(relaxed = true) lateinit var eventManager: EventManager
    @MockK(relaxed = true) lateinit var clock: Clock

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = LobbyImpressionAnalytics("payloaId", followManager, eventManager, clock)
    }

    @Test
    fun `follow clicked sends correct event`() = runBlockingTest {
        val list = mutableListOf<EventBody>()
        coEvery { eventManager.sendEvent(capture(list)) } just Runs
        subject.followClicked(mockk(relaxed = true))
        assertEquals(1, list.size)
        assertTrue(list.first() is LobbyFollowClickedEvent)
    }

    @Test
    fun `card clicked sends correct event`() = runBlockingTest {
        val list = mutableListOf<EventBody>()
        coEvery { eventManager.sendEvent(capture(list)) } just Runs
        subject.cardClicked(mockk(relaxed = true))
        assertEquals(1, list.size)
        assertTrue(list.first() is LobbyCardClickedEvent)
    }

    @Test
    fun `card impression sends correct event`() = runBlockingTest {
        val list = mutableListOf<EventBody>()
        coEvery { eventManager.sendEvent(capture(list)) } just Runs
        subject.sendImpressionEventData(mockk(relaxed = true))
        assertEquals(1, list.size)
        assertTrue(list.first() is LobbyImpressionEvent)
    }
}

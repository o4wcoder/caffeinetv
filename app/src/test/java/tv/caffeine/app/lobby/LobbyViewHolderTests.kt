package tv.caffeine.app.lobby

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.picasso.Picasso
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.Clock
import tv.caffeine.app.analytics.EventManager
import tv.caffeine.app.api.LobbyCardClickedEvent
import tv.caffeine.app.api.LobbyFollowClickedEvent
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.LiveBroadcastCardBinding
import tv.caffeine.app.databinding.LiveBroadcastWithFriendsCardBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.loadAvatar
import tv.caffeine.app.util.UserTheme
import java.lang.IllegalStateException

@RunWith(RobolectricTestRunner::class)
class LobbyViewHolderTests {

    private val couroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var liveBroadcastCardBinding: LiveBroadcastCardBinding
    private lateinit var liveBroadcastWithFriendsCardBinding: LiveBroadcastWithFriendsCardBinding

    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var clock: Clock
    @MockK(relaxed = true) lateinit var eventManager: EventManager
    @MockK(relaxed = true) lateinit var watcherUser: User
    @MockK(relaxed = true) lateinit var broadcasterUser: User
    @MockK(relaxed = true) lateinit var broadcast: Broadcast
    @MockK(relaxed = true) lateinit var broadcaster: Lobby.Broadcaster
    @MockK(relaxed = true) lateinit var picasso: Picasso
    @MockK(relaxed = true) lateinit var liveBroadcast: LiveBroadcast
    @MockK(relaxed = true) lateinit var liveBroadcastWithFriends: LiveBroadcastWithFriends

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val context = InstrumentationRegistry.getInstrumentation().context
        liveBroadcastCardBinding = LiveBroadcastCardBinding.inflate(
            LayoutInflater.from(context), FrameLayout(context), false)
        liveBroadcastWithFriendsCardBinding = LiveBroadcastWithFriendsCardBinding.inflate(
            LayoutInflater.from(context), FrameLayout(context), false)

        every { clock.millis() } returns 123000L
        every { watcherUser.caid } returns "caid123"
        every { broadcasterUser.stageId } returns "456"
        every { broadcaster.broadcast } returns broadcast
        every { broadcaster.followingViewers } returns listOf()
        every { broadcaster.user } returns broadcasterUser
        every { followManager.isFollowing(any()) } returns false
        every { followManager.followersLoaded() } returns true
        every { followManager.currentUserDetails() } returns watcherUser
        coEvery { followManager.followUser(any(), any()) } returns CaffeineEmptyResult.Success
        every { liveBroadcast.broadcaster } returns broadcaster
        every { liveBroadcastWithFriends.broadcaster } returns broadcaster
        mockkStatic("tv.caffeine.app.ui.PicassoExtKt")
    }

    // Follow button clicked
    @Test
    fun `clicking the follow button on a live broadcast card logs the follow event`() {
        val card = createLiveBroadcastCard("0")
        every { card.binding.avatarImageView.loadAvatar(any(), any(), any()) } returns Unit

        card.bind(liveBroadcast)
        card.binding.followButton.performClick()
        coVerify(exactly = 1) { eventManager.sendEvent(any<LobbyFollowClickedEvent>()) }
    }

    @Test
    fun `clicking the follow button on a live broadcast with friends card logs the follow event`() {
        val card = createLiveBroadcastWithFriendsCard("0")
        every { card.binding.avatarImageView.loadAvatar(any(), any(), any()) } returns Unit

        card.bind(liveBroadcastWithFriends)
        card.binding.followButton.performClick()
        coVerify(exactly = 1) { eventManager.sendEvent(any<LobbyFollowClickedEvent>()) }
    }

    // Broadcast card clicked
    @Test
    fun `clicking the preview image on a live broadcast card logs the card clicked event`() {
        val card = createLiveBroadcastCard("0")
        every { card.binding.avatarImageView.loadAvatar(any(), any(), any()) } returns Unit

        card.bind(liveBroadcast)
        try {
            card.binding.previewImageView.performClick()
        } catch (e: IllegalStateException) {
            // NavController not set
        }
        coVerify(exactly = 1) { eventManager.sendEvent(any<LobbyCardClickedEvent>()) }
    }

    @Test
    fun `clicking the preview image on a live broadcast with friends card logs the card clicked event`() {
        val card = createLiveBroadcastWithFriendsCard("0")
        every { card.binding.avatarImageView.loadAvatar(any(), any(), any()) } returns Unit

        card.bind(liveBroadcastWithFriends)
        try {
            card.binding.previewImageView.performClick()
        } catch (e: IllegalStateException) {
            // NavController not set
        }
        coVerify(exactly = 1) { eventManager.sendEvent(any<LobbyCardClickedEvent>()) }
    }

    // Event data
    @Test
    fun `the lobby clicked event data matches the live broadcast card data`() {
        val card = createLiveBroadcastCard("lobby123")
        val eventData = card.getLobbyClickedEventData(liveBroadcast)
        assertEquals("lobby123", eventData?.payloadId)
        assertEquals("caid123", eventData?.caid)
        assertEquals("456", eventData?.stageId)
        assertEquals(123000L, eventData?.clickedAt)
    }

    @Test
    fun `the lobby clicked event data matches the live broadcast with friends card data`() {
        val card = createLiveBroadcastWithFriendsCard("lobby123")
        val eventData = card.getLobbyClickedEventData(liveBroadcast)
        assertEquals("lobby123", eventData?.payloadId)
        assertEquals("caid123", eventData?.caid)
        assertEquals("456", eventData?.stageId)
        assertEquals(123000L, eventData?.clickedAt)
    }

    private fun createLiveBroadcastCard(payloadId: String): LiveBroadcastCard {
        val theme = mockk<UserTheme>(relaxed = true)
        return LiveBroadcastCard(liveBroadcastCardBinding, mapOf(), mapOf(), followManager,
            theme, theme, theme, theme, picasso, payloadId, couroutineScope, clock, eventManager)
    }

    private fun createLiveBroadcastWithFriendsCard(payloadId: String): LiveBroadcastWithFriendsCard {
        val theme = mockk<UserTheme>(relaxed = true)
        return LiveBroadcastWithFriendsCard(
            liveBroadcastWithFriendsCardBinding, mapOf(), mapOf(), followManager,
            theme, theme, theme, theme, picasso, payloadId, couroutineScope, clock, eventManager
        )
    }
}

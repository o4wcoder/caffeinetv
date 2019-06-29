package tv.caffeine.app.lobby.release

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.GlobalScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.LiveBroadcast
import tv.caffeine.app.session.FollowManager

@RunWith(RobolectricTestRunner::class)
class LobbyCardsOnlineBroadcasterTests {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    private val genericUser = User("caid", "username", "name", "email", "/avatarImagePath", 0, 0, false, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)

    lateinit var context: Context
    @MockK lateinit var followManager: FollowManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun `clicking the card navigates to the stage`() {
        val broadcast = Broadcast("id", "name", "contentId", null, null,
            "pip", Broadcast.State.ONLINE, "date", null, null)
        val user = genericUser
        val broadcaster = Lobby.Broadcaster("2", "OnlineBroadcaster", user, "tag", broadcast, null,
            listOf(), 0, 0, null)
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        every { followManager.isFollowing(any()) } returns false
        coEvery { followManager.followUser(any()) } returns mockk()
        val onlineBroadcaster = OnlineBroadcaster(context, followManager, liveBroadcast, GlobalScope)
        onlineBroadcaster.cardClicked()
        onlineBroadcaster.navigationCommands.observeForever {
            val navigationCommand = it.peekContent()
            assertTrue(navigationCommand is NavigationCommand.To)
            val directions = (navigationCommand as NavigationCommand.To).directions
            assertEquals(R.id.action_lobbySwipeFragment_to_stagePagerFragment, directions.actionId)
        }
    }

    @Test
    fun `clicking follow button calls follow manager`() {
        val broadcast = Broadcast("id", "name", "contentId", null, null,
            "pip", Broadcast.State.ONLINE, "date", null, null)
        val user = genericUser
        val broadcaster = Lobby.Broadcaster("2", "OnlineBroadcaster", user, "tag", broadcast, null,
            listOf(), 0, 0, null)
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        every { followManager.isFollowing(any()) } returns false
        coEvery { followManager.followUser(any()) } returns mockk()
        val onlineBroadcaster = OnlineBroadcaster(context, followManager, liveBroadcast, GlobalScope)
        onlineBroadcaster.followClicked()
        coVerify { followManager.followUser(any()) }
    }
}

package tv.caffeine.app.lobby.release

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.GlobalScope
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import tv.caffeine.app.R
import tv.caffeine.app.analytics.LobbyImpressionAnalytics
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.PreviousBroadcast
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.makeGenericUser
import tv.caffeine.app.util.makeOfflineBroadcast

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LobbyCardsOfflineBroadcasterTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    lateinit var context: Context
    lateinit var previousBroadcast: PreviousBroadcast
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var lobbyImpressionAnalytics: LobbyImpressionAnalytics

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = InstrumentationRegistry.getInstrumentation().context
        previousBroadcast = makePreviousBroadcast()
        coEvery { lobbyImpressionAnalytics.sendImpressionEventData(any()) } just Runs
    }

    @Test
    fun `clicking the card navigates to the stage`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { lobbyImpressionAnalytics.cardClicked(any()) } just Runs
        val offlineBroadcaster = makeOfflineBroadcaster(previousBroadcast)

        offlineBroadcaster.cardClicked()

        offlineBroadcaster.navigationCommands.observeForever {
            val navigationCommand = it.peekContent()
            Assert.assertTrue(navigationCommand is NavigationCommand.To)
            val directions = (navigationCommand as NavigationCommand.To).directions
            Assert.assertEquals(R.id.action_lobbySwipeFragment_to_stagePagerFragment, directions.actionId)
        }
    }

    @Test
    fun `clicking follow button calls follow manager`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { followManager.followUser(any()) } returns mockk()
        every { followManager.currentUserDetails() } returns mockk(relaxed = true)
        coEvery { lobbyImpressionAnalytics.followClicked(any()) } just Runs
        val offlineBroadcaster = makeOfflineBroadcaster(previousBroadcast)

        offlineBroadcaster.followClicked()

        coVerify { followManager.followUser("caid") }
    }

    @Test
    fun `clicking follow button on a non-followed user sends analytics`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { followManager.followUser(any()) } returns mockk()
        val user = mockk<User>()
        every { user.caid } returns "123"
        every { followManager.currentUserDetails() } returns user
        coEvery { lobbyImpressionAnalytics.followClicked(any()) } just Runs
        val offlineBroadcaster = makeOfflineBroadcaster(previousBroadcast)

        offlineBroadcaster.followClicked()

        coVerify { lobbyImpressionAnalytics.followClicked(any()) }
    }

    @Test
    fun `clicking card sends lobby card click analytics`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { lobbyImpressionAnalytics.cardClicked(any()) } just Runs
        val offlineBroadcaster = makeOfflineBroadcaster(previousBroadcast)

        offlineBroadcaster.cardClicked()

        coVerify { lobbyImpressionAnalytics.cardClicked(any()) }
    }

    private fun makePreviousBroadcast(): PreviousBroadcast {
        val genericUser = makeGenericUser()
        val offlineBroadcast = makeOfflineBroadcast()
        val broadcaster = Lobby.Broadcaster("2", "OnlineBroadcaster", genericUser, "tag", null, offlineBroadcast,
            listOf(), 0, 0, null)
        val previousBroadcast = PreviousBroadcast("1", broadcaster)
        return previousBroadcast
    }

    private fun makeOfflineBroadcaster(previousBroadcast: PreviousBroadcast): OfflineBroadcaster {
        val offlineBroadcaster = OfflineBroadcaster(
            followManager,
            previousBroadcast.broadcaster,
            lobbyImpressionAnalytics,
            GlobalScope
        )
        return offlineBroadcaster
    }
}

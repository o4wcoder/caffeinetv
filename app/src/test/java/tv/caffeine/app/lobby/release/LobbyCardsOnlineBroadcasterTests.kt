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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import tv.caffeine.app.R
import tv.caffeine.app.analytics.LobbyImpressionAnalytics
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.LiveBroadcast
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.makeBroadcaster
import tv.caffeine.app.util.makeGenericUser
import tv.caffeine.app.util.makeOnlineBroadcast
import tv.caffeine.app.test.observeForTesting

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LobbyCardsOnlineBroadcasterTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    lateinit var context: Context
    lateinit var liveBroadcast: LiveBroadcast
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var lobbyImpressionAnalytics: LobbyImpressionAnalytics

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = InstrumentationRegistry.getInstrumentation().context
        liveBroadcast = makeLiveBroadcast()
        every { followManager.isSelf(any()) } returns false
        coEvery { lobbyImpressionAnalytics.sendImpressionEventData(any()) } just Runs
    }

    @Test
    fun `clicking the card navigates to the stage`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { lobbyImpressionAnalytics.cardClicked(any()) } just Runs
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)

        onlineBroadcaster.cardClicked()

        onlineBroadcaster.navigationCommands.observeForTesting {
            val navigationCommand = it.peekContent()
            assertTrue(navigationCommand is NavigationCommand.To)
            val directions = (navigationCommand as NavigationCommand.To).directions
            assertEquals(R.id.action_global_stagePagerFragment, directions.actionId)
        }
    }

    @Test
    fun `clicking follow button calls follow manager`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { followManager.followUser(any()) } returns mockk()
        every { followManager.currentUserDetails() } returns mockk(relaxed = true)
        coEvery { lobbyImpressionAnalytics.followClicked(any()) } just Runs
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)

        onlineBroadcaster.followClicked()

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
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)

        onlineBroadcaster.followClicked()

        coVerify { lobbyImpressionAnalytics.followClicked(any()) }
    }

    @Test
    fun `clicking card sends lobby card click analytics`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { lobbyImpressionAnalytics.cardClicked(any()) } just Runs
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)

        onlineBroadcaster.cardClicked()

        coVerify { lobbyImpressionAnalytics.cardClicked(any()) }
    }

    @Test
    fun `clicking the kebab button navigates to the report-ignore dialog`() {
        every { followManager.isFollowing(any()) } returns false
        coEvery { lobbyImpressionAnalytics.cardClicked(any()) } just Runs
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)

        onlineBroadcaster.kebabClicked()

        onlineBroadcaster.navigationCommands.observeForTesting {
            val navigationCommand = it.peekContent()
            assertTrue(navigationCommand is NavigationCommand.To)
            val directions = (navigationCommand as NavigationCommand.To).directions
            assertEquals(R.id.action_global_reportOrIgnoreDialogFragment, directions.actionId)
        }
    }

    private fun makeLiveBroadcast(): LiveBroadcast {
        val genericUser = makeGenericUser()
        val onlineBroadcast = makeOnlineBroadcast()
        val broadcaster = makeBroadcaster(genericUser, onlineBroadcast)
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        return liveBroadcast
    }

    private fun makeOnlineBroadcaster(liveBroadcast: LiveBroadcast): OnlineBroadcaster {
        val onlineBroadcaster = OnlineBroadcaster(
            context,
            followManager,
            liveBroadcast.broadcaster,
            lobbyImpressionAnalytics,
            GlobalScope
        )
        return onlineBroadcaster
    }
}

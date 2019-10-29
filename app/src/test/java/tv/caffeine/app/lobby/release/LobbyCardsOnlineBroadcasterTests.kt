package tv.caffeine.app.lobby.release

import android.content.Context
import android.view.View
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.analytics.LobbyImpressionAnalytics
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.LiveBroadcast
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.test.observeForTesting
import tv.caffeine.app.util.CoroutinesTestRule
import tv.caffeine.app.util.makeBroadcaster
import tv.caffeine.app.util.makeGenericUser
import tv.caffeine.app.util.makeOnlineBroadcast

@RunWith(RobolectricTestRunner::class)
class LobbyCardsOnlineBroadcasterTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    lateinit var context: Context
    private lateinit var liveBroadcast: LiveBroadcast
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

    @Test
    fun `show the badge text instead of the friends watching or live text if the badge text is not null`() {
        // TODO: The friends watching badge will be a custom view. This test will be re-written soon.
        every { followManager.isFollowing(any()) } returns false
        val badgeText = "Top pick"
        val genericUser = makeGenericUser()
        val onlineBroadcast = makeOnlineBroadcast()
        val broadcaster = makeBroadcaster(genericUser, onlineBroadcast, badgeText)
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)
        assertEquals(badgeText, onlineBroadcaster.badgeText)
        assertEquals(View.GONE, onlineBroadcaster.liveBadgeIndicatorVisibility)
        assertEquals(View.VISIBLE, onlineBroadcaster.liveBadgeTextVisibility)
    }

    @Test
    fun `show the friends watching text instead of the badge text if the badge text is null`() {
        // TODO: The friends watching badge will be a custom view. This test will be re-written soon.
        every { followManager.isFollowing(any()) } returns false
        val badgeText = null
        val genericUser = makeGenericUser()
        val onlineBroadcast = makeOnlineBroadcast()
        val followingViewers = listOf(genericUser)
        val broadcaster = makeBroadcaster(genericUser, onlineBroadcast, badgeText, null, followingViewers)
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)
        assertEquals("username", onlineBroadcaster.badgeText)
        assertEquals(View.GONE, onlineBroadcaster.liveBadgeIndicatorVisibility)
        assertEquals(View.VISIBLE, onlineBroadcaster.liveBadgeTextVisibility)
    }

    @Test
    fun `show the live indicator instead of the badge text if the badge text is null and no friends watching`() {
        // TODO: The friends watching badge will be a custom view. This test will be re-written soon.
        every { followManager.isFollowing(any()) } returns false
        val badgeText = null
        val genericUser = makeGenericUser()
        val onlineBroadcast = makeOnlineBroadcast()
        val broadcaster = makeBroadcaster(genericUser, onlineBroadcast, badgeText)
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)
        assertNull(onlineBroadcaster.badgeText)
        assertEquals(View.VISIBLE, onlineBroadcaster.liveBadgeIndicatorVisibility)
        assertEquals(View.GONE, onlineBroadcaster.liveBadgeTextVisibility)
    }

    @Test
    fun `show the age restriction badge if there is an age restriction`() {
        every { followManager.isFollowing(any()) } returns false
        val genericUser = makeGenericUser()
        val onlineBroadcast = makeOnlineBroadcast()
        val broadcaster = makeBroadcaster(genericUser, onlineBroadcast, null, "17+")
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)
        assertEquals("17+", onlineBroadcaster.ageRestriction)
        assertEquals(View.VISIBLE, onlineBroadcaster.ageRestrictionVisibility)
    }

    @Test
    fun `do not show the age restriction badge if there is not an age restriction`() {
        every { followManager.isFollowing(any()) } returns false
        val genericUser = makeGenericUser()
        val onlineBroadcast = makeOnlineBroadcast()
        val broadcaster = makeBroadcaster(genericUser, onlineBroadcast, null, null)
        val liveBroadcast = LiveBroadcast("1", broadcaster)
        val onlineBroadcaster = makeOnlineBroadcaster(liveBroadcast)
        assertNull(onlineBroadcaster.ageRestriction)
        assertEquals(View.GONE, onlineBroadcaster.ageRestrictionVisibility)
    }

    private fun makeLiveBroadcast(): LiveBroadcast {
        val genericUser = makeGenericUser()
        val onlineBroadcast = makeOnlineBroadcast()
        val broadcaster = makeBroadcaster(genericUser, onlineBroadcast)
        return LiveBroadcast("1", broadcaster)
    }

    private fun makeOnlineBroadcaster(liveBroadcast: LiveBroadcast) =
        OnlineBroadcaster(
            context,
            followManager,
            liveBroadcast.broadcaster,
            lobbyImpressionAnalytics,
            GlobalScope
        )
}

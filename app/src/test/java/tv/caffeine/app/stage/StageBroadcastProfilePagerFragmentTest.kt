package tv.caffeine.app.stage

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.session.FollowManager

private inline fun launchStageBroadcastPagerFragmentWithArgs(
    followerCountString: String?,
    followingCountString: String?,
    followManager: FollowManager,
    crossinline action: (StageBroadcastProfilePagerFragment) -> Unit
) {
    val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
    val testComponent = DaggerTestComponent.factory().create(app)
    app.setApplicationInjector(testComponent)
    val arguments = StageBroadcastProfilePagerFragmentArgs("username", "caid", followerCountString, followingCountString).toBundle()
    val navController = mockk<NavController>(relaxed = true)
    val adapterFactory = mockk<StageBroadcastProfilePagerAdapter_AssistedFactory>(relaxed = true)
    val scenario = launchFragmentInContainer(arguments, R.style.AppTheme) {
        StageBroadcastProfilePagerFragment(adapterFactory, followManager).also {
            it.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    // The fragment’s view has just been created
                    Navigation.setViewNavController(it.requireView(), navController)
                }
            }
        }
    }
    scenario.onFragment {
        action(it)
    }
}

private val context = ApplicationProvider.getApplicationContext<CaffeineApplication>()

@RunWith(RobolectricTestRunner::class)
class StageBroadcastProfilePagerFragmentTest {

    lateinit var subject: StageBroadcastProfilePagerFragment

    @MockK private lateinit var followManager: FollowManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { followManager.isSelf(any()) } returns false
    }

    @Test
    fun `followers tab title is correct with null followers`() {
        launchStageBroadcastPagerFragmentWithArgs(null, null, followManager) {
            subject = it
        }
        assertEquals(context.getString(R.string.stage_broadcast_followers_tab), subject.getFollowersTabTitle())
        assertEquals(context.getString(R.string.stage_broadcast_following_tab), subject.getFollowingTabTitle())
    }

    @Test
    fun `followers tab title is correct with numbered followers`() {
        val followers = "42"
        val following = "99.9K"
        launchStageBroadcastPagerFragmentWithArgs(followers, following, followManager) {
            subject = it
        }
        assertEquals(context.resources.getQuantityString(R.plurals.numbered_stage_broadcast_followers_tab, 2, followers), subject.getFollowersTabTitle())
        assertEquals(context.getString(R.string.numbered_stage_broadcast_following_tab, following), subject.getFollowingTabTitle())
    }

    @Test
    fun `the gift button is visible if the broadcaster is not me`() {
        every { followManager.isSelf(any()) } returns false
        launchStageBroadcastPagerFragmentWithArgs("0", "0", followManager) {
            subject = it
        }
        assertEquals(View.VISIBLE, subject.getGiftButtonVisibility())
    }

    @Test
    fun `the gift button is invisible if the broadcaster is me`() {
        every { followManager.isSelf(any()) } returns true
        launchStageBroadcastPagerFragmentWithArgs("0", "0", followManager) {
            subject = it
        }
        assertEquals(View.INVISIBLE, subject.getGiftButtonVisibility())
    }
}
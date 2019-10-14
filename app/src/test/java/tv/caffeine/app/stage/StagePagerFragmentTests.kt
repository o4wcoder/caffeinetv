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
import tv.caffeine.app.api.model.User
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig

@RunWith(RobolectricTestRunner::class)
class StagePagerFragmentTests {

    lateinit var fragment: StagePagerFragment
    private val swipeButtonOnClickListener = View.OnClickListener {}

    @MockK lateinit var currentUser: User
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var releaseDesignConfig: ReleaseDesignConfig
    @MockK lateinit var adapterFactory: StagePagerAdapter.Factory
    @MockK(relaxed = true) lateinit var stagePagerAdapter: StagePagerAdapter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { currentUser.username } returns "me"
        every { followManager.currentUserDetails() } returns currentUser
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        every { adapterFactory.create(any(), any(), any()) } returns stagePagerAdapter
        launchStagePagerFragmentWithArgs("user1", arrayOf("user0", "user1", "user2")) {
            fragment = it
            fragment.binding = null
        }
    }

    @Test
    fun `the index of the stage among swipeable stages is the same index in the lobby if it exists there`() {
        val initialBroadcaster = "b"
        val lobbyBroadcasters = listOf("a", "b", "c")
        val (broadcasters, index) = fragment.configureBroadcasters(initialBroadcaster, lobbyBroadcasters)
        assertEquals(listOf("a", "b", "c"), broadcasters)
        assertEquals(1, index)
    }

    @Test
    fun `the index of the stage among swipeable stages is 0 if it does not exist in the lobby`() {
        val initialBroadcaster = "d"
        val lobbyBroadcasters = listOf("a", "b", "c")
        val (broadcasters, index) = fragment.configureBroadcasters(initialBroadcaster, lobbyBroadcasters)
        assertEquals(listOf("d", "a", "b", "c"), broadcasters)
        assertEquals(0, index)
    }

    @Test
    fun `disable swiping between broadcasts if the user lands on their own stage`() {
        every { currentUser.username } returns "user1"
        fragment.setupAdapter(swipeButtonOnClickListener)
        assertEquals(1, fragment.broadcasters.size)
        assertEquals("user1", fragment.broadcasters[0])
    }

    @Test
    fun `the broadcasters and index are set correctly if they are not null`() {
        fragment.setupAdapter(swipeButtonOnClickListener)
        assertEquals(3, fragment.broadcasters.size)
        assertEquals("user0", fragment.broadcasters[0])
        assertEquals("user1", fragment.broadcasters[1])
        assertEquals("user2", fragment.broadcasters[2])
    }

    private inline fun launchStagePagerFragmentWithArgs(
        broadcasterUsername: String,
        broadcasters: Array<String>?,
        crossinline action: (StagePagerFragment) -> Unit
    ) {
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        val arguments = StagePagerFragmentArgs(broadcasterUsername, broadcasters).toBundle()
        val navController = mockk<NavController>(relaxed = true)
        val scenario = launchFragmentInContainer(arguments, R.style.AppTheme) {
            StagePagerFragment(mockk(), adapterFactory, followManager, releaseDesignConfig).also {
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
}

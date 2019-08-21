package tv.caffeine.app.stage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.stage.classic.ClassicChatFragment
import tv.caffeine.app.stage.release.ReleaseChatFragment

private inline fun launchStageFragmentWithArgs(
    broadcastUsername: String,
    canSwipe: Boolean,
    crossinline action: (StageFragment) -> Unit
) {
    val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
    val testComponent = DaggerTestComponent.factory().create(app)
    app.setApplicationInjector(testComponent)
    val arguments = StageFragmentArgs(broadcastUsername, canSwipe).toBundle()
    val navController = mockk<NavController>(relaxed = true)
    val scenario = launchFragmentInContainer(arguments, R.style.AppTheme) {
        StageFragment(mockk(), mockk(relaxed = true), mockk(), mockk(), mockk(relaxed = true)).also {
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

@RunWith(RobolectricTestRunner::class)
class StageFragmentVisibilityTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    @MockK private lateinit var releaseDesignConfig: ReleaseDesignConfig

    lateinit var subject: StageFragment

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        launchStageFragmentWithArgs("username", true) {
            subject = it
        }
    }

    @Test
    fun `hiding overlays hides app bar`() {
        subject.binding.stageAppbar.isVisible = true
        subject.hideOverlays()
        assertFalse(subject.binding.stageAppbar.isVisible)
    }

    @Test
    fun `showing overlays shows app bar if it's included`() {
        subject.binding.stageAppbar.isVisible = false
        subject.showOverlays(true)
        assertTrue(subject.binding.stageAppbar.isVisible)
    }

    @Test
    fun `showing overlays does not show the app bar if it's not included`() {
        subject.binding.stageAppbar.isVisible = false
        subject.showOverlays(false)
        assertFalse(subject.binding.stageAppbar.isVisible)
    }

    @Test
    fun `showing overlays on an offline stage does not show live indicator`() {
        subject.stageIsLive = false
        subject.showOverlays()
        assertFalse(subject.binding.liveIndicatorTextView.isVisible)
    }

    @Test
    fun `showing overlays on a live stage with good quality shows live indicator`() {
        subject.stageIsLive = true
        subject.feedQuality = FeedQuality.GOOD
        subject.showOverlays()
        assertTrue(subject.binding.liveIndicatorTextView.isVisible)
    }

    @Test
    fun `showing overlays on a live stage with poor quality shows live indicator`() {
        subject.stageIsLive = true
        subject.feedQuality = FeedQuality.POOR
        subject.showOverlays()
        assertTrue(subject.binding.liveIndicatorTextView.isVisible)
    }

    @Test
    fun `showing overlays on a live stage with poor quality shows weak connection container`() {
        subject.stageIsLive = true
        subject.feedQuality = FeedQuality.POOR
        subject.showOverlays()
        assertTrue(subject.binding.weakConnectionContainer.isVisible)
    }

    @Test
    fun `showing overlays on a live stage with good quality does not show weak connection overlay`() {
        subject.stageIsLive = true
        subject.feedQuality = FeedQuality.GOOD
        subject.showOverlays()
        assertFalse(subject.binding.weakConnectionContainer.isVisible)
    }

    @Test
    fun `poor network quality shows blinking no network data indicator`() {
        subject.stageIsLive = true
        subject.feedQuality = FeedQuality.POOR
        subject.updatePoorConnectionAnimation()
        assertTrue(subject.binding.poorConnectionPulseImageView.isVisible)
    }

    @Test
    fun `good network quality does not show blinking no network data indicator`() {
        subject.stageIsLive = true
        subject.feedQuality = FeedQuality.GOOD
        subject.updatePoorConnectionAnimation()
        assertFalse(subject.binding.poorConnectionPulseImageView.isVisible)
    }

    @Test
    fun `showing overlays on a live stage with poor quality hides blinking no network data indicator`() {
        subject.stageIsLive = true
        subject.feedQuality = FeedQuality.POOR
        subject.updatePoorConnectionAnimation()
        subject.showOverlays()
        assertFalse(subject.binding.poorConnectionPulseImageView.isVisible)
    }

    @Test
    fun `showing overlays on an offline stage does not show game logo`() {
        subject.stageIsLive = false
        subject.showOverlays()
        assertFalse(subject.binding.gameLogoImageView.isVisible)
    }

    @Test
    fun `showing overlays on a live stage shows game logo`() {
        subject.stageIsLive = true
        subject.showOverlays()
        assertTrue(subject.binding.gameLogoImageView.isVisible)
    }

    @Test
    fun `hiding overlays on an offline stage hides game logo`() {
        subject.stageIsLive = true
        subject.hideOverlays()
        assertFalse(subject.binding.gameLogoImageView.isVisible)
    }

    @Test
    fun `showing overlays on a live stage shows avatar username container`() {
        subject.stageIsLive = true
        subject.showOverlays()
        assertTrue(subject.binding.avatarUsernameContainer.isVisible)
    }

    @Test
    fun `showing overlays on an offline stage shows avatar username container`() {
        subject.stageIsLive = false
        subject.showOverlays()
        assertTrue(subject.binding.avatarUsernameContainer.isVisible)
    }

    @Test
    fun `showing overlays does not change the visibility of the follow button`() {
        subject.showOverlays()
        // verify(exactly = 0) { subject.binding.followButton.visibility = any() }
    }

    @Test
    fun `hiding overlays does not change the visibility of the follow button`() {
        subject.hideOverlays()
        // verify(exactly = 0) { subject.binding.followButton.visibility = any() }
    }

    @Test
    fun `stage going offline shows offline views`() {
        subject.updateBroadcastOnlineState(false)
        assertTrue(subject.binding.showIsOverTextView.isVisible)
        assertTrue(subject.binding.backToLobbyButton.isVisible)
    }

    @Test
    fun `stage going live hides offline views`() {
        subject.updateBroadcastOnlineState(true)
        assertFalse(subject.binding.showIsOverTextView.isVisible)
        assertFalse(subject.binding.backToLobbyButton.isVisible)
    }

    @Test
    fun `the swipe button is visible if the stage is allowed to swipe`() {
        subject.configureButtons()
        assertTrue(subject.binding.swipeButton.isVisible)
    }

    @Test
    fun `the poor connection overlay should be visible when feedQuality is BAD`() {
        subject.feedQuality = FeedQuality.BAD
        subject.updateBadConnectionOverlay()
        assertTrue(subject.binding.badConnectionContainer.isVisible)
    }

    @Test
    fun `the poor connection overlay should be gone when feedQuality is POOR`() {
        subject.feedQuality = FeedQuality.POOR
        subject.updateBadConnectionOverlay()
        assertTrue(!subject.binding.badConnectionContainer.isVisible)
    }

    @Test
    fun `the poor connection overlay should be gone when feedQuality is GOOD`() {
        subject.feedQuality = FeedQuality.GOOD
        subject.updateBadConnectionOverlay()
        assertTrue(!subject.binding.badConnectionContainer.isVisible)
    }

    @Test
    fun `show that bottom container contains chat fragment`() {
        subject.updateBottomFragment(BottomContainerType.CHAT)
        val chatFragment =
            subject.childFragmentManager.findFragmentById(R.id.bottom_fragment_container) as ChatFragment
        assertNotNull(chatFragment)
    }

    @Test
    fun `when release config is set show release chat fragment`() {
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns true
        subject.updateBottomFragment(BottomContainerType.CHAT)
        val chatFragment =
            subject.childFragmentManager.findFragmentById(R.id.bottom_fragment_container) as ReleaseChatFragment
        assertNotNull(chatFragment)
    }

    @Test
    fun `when release config is not set show classic chat fragment`() {
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns false
        subject.updateBottomFragment(BottomContainerType.CHAT)
        val chatFragment =
            subject.childFragmentManager.findFragmentById(R.id.bottom_fragment_container) as ClassicChatFragment
        assertNotNull(chatFragment)
    }

    @Test
    fun `when release config is set show broadcast details fragment`() {
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns true
        subject.updateBottomFragment(BottomContainerType.PROFILE)
        val stageBroadcastDetailsPagerFragment =
            subject.childFragmentManager.findFragmentById(R.id.bottom_fragment_container) as StageBroadcastDetailsPagerFragment
        assertNotNull(stageBroadcastDetailsPagerFragment)
    }
}

@RunWith(RobolectricTestRunner::class)
class StageFragmentSwipeNotAllowedTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    lateinit var subject: StageFragment

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        launchStageFragmentWithArgs("ABC", false) {
            subject = it
        }
    }

    @Test
    fun `the swipe button is invisible if the stage cannot swipe`() {
        subject.configureButtons()
        assertFalse(subject.binding.swipeButton.isVisible)
    }
}

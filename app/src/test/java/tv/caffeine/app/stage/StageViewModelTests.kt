package tv.caffeine.app.stage

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.settings.ReleaseDesignConfig

class StageViewModelTests {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var subject: StageViewModel

    @MockK
    lateinit var fakeReleaseDesignConfig: ReleaseDesignConfig

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { fakeReleaseDesignConfig.isReleaseDesignActive() } returns true

        subject = StageViewModel(fakeReleaseDesignConfig, mockk())
    }

    @Test
    fun `showing overlays on a live stage with good quality shows live indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertTrue(subject.getLiveIndicatorVisibility())
    }

    @Test
    fun `showing overlays on a live stage with poor quality shows live indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.POOR)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertTrue(subject.getLiveIndicatorVisibility())
    }

    @Test
    fun `showing overlays on an offline stage does not show live indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getLiveIndicatorVisibility())
    }

    @Test
    fun `showing overlays with no friends watching on shows the live indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateHasFriendsWatching(false)
        subject.updateIsMe(false)
        assertTrue(subject.getLiveIndicatorVisibility())
    }

    @Test
    fun `showing overlays with friends watching on live stage does not show live indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateHasFriendsWatching(true)
        subject.updateIsMe(false)
        assertFalse(subject.getLiveIndicatorVisibility())
    }

    @Test
    fun `showing overlays with friends watching on shows the friends watching indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateHasFriendsWatching(true)
        subject.updateIsMe(false)
        assertTrue(subject.getFriendsWatchingIndicatorVisiblility())
    }

    @Test
    fun `showing overlays on a live release stage with good quality hides classic live indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertFalse(subject.getClassicLiveIndicatorTextViewVisibility())
    }

    @Test
    fun `showing overlays on an offline stage does not show game logo`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getGameLogoVisibility())
    }

    @Test
    fun `showing overlays on a live stage shows game logo`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertTrue(subject.getGameLogoVisibility())
    }

    @Test
    fun `hiding overlays on an offline stage hides game logo`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getGameLogoVisibility())
    }

    @Test
    fun `showing overlays on an offline stage shows swipe button`() {
        subject.isReleaseDesign.set(false)
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertTrue(subject.getSwipeButtonVisibility())
    }

    @Test
    fun `hiding overlays hides the live indicator and avatar container`() {
        subject.updateOverlayIsVisible(false, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertFalse(subject.getLiveIndicatorAndAvatarContainerVisibility())
    }

    @Test
    fun `showing overlays shows the live indicator and avatar container`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertTrue(subject.getLiveIndicatorAndAvatarContainerVisibility())
    }

    @Test
    fun `hiding overlays hides app bar`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        subject.updateOverlayIsVisible(false, false)
        assertFalse(subject.getAppBarVisibility())
    }

    @Test
    fun `showing overlays shows app bar if it's included`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertTrue(subject.getAppBarVisibility())
    }

    @Test
    fun `showing overlays does not show the app bar if it's not included`() {
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertFalse(subject.getAppBarVisibility())
    }

    @Test
    fun `showing overlays on a live stage shows avatar username container when user is not me`() {
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(false)
        assertTrue(subject.getAvatarUsernameContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage hides avatar username container when user is me`() {
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateIsMe(true)
        assertFalse(subject.getAvatarUsernameContainerVisibility())
    }

    @Test
    fun `showing overlays on an offline stage hides avatar username container when user is me`() {
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(true)
        assertFalse(subject.getAvatarUsernameContainerVisibility())
    }

    @Test
    fun `showing overlays on an classic offline stage shows avatar username container`() {
        subject.isReleaseDesign.set(false)
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertTrue(subject.getAvatarUsernameContainerVisibility())
    }

    @Test
    fun `showing overlays on an offline stage does not show avatar username container`() {
        subject.isReleaseDesign.set(true)
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getAvatarUsernameContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with poor quality shows weak connection container`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.POOR)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertTrue(subject.getWeakConnnectionContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with good quality does not show weak connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getWeakConnnectionContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with bad quality does not show weak connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.BAD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getWeakConnnectionContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with bad quality shows bad connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.BAD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertTrue(subject.getBadConnectionOverlayVisibility())
    }

    @Test
    fun `showing overlays on a live stage with good quality hides the bad connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getBadConnectionOverlayVisibility())
    }

    @Test
    fun `showing overlays on a live stage with poor quality hides the bad connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.POOR)
        subject.updateStageIsLive(false)
        subject.updateIsMe(false)
        assertFalse(subject.getBadConnectionOverlayVisibility())
    }

    @Test
    fun `showing offline profile overlay when stage is not live`() {
        subject.isProfileOverlayVisible = true
        assertEquals(subject.getProfileOverlayVisibility(), View.VISIBLE)
    }

    @Test
    fun `not showing offline profile overlay when stage is live`() {
        subject.isProfileOverlayVisible = false
        assertEquals(subject.getProfileOverlayVisibility(), View.GONE)
    }
}
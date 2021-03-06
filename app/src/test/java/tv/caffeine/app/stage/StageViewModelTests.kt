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
import tv.caffeine.app.R
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.stream.type.ContentRating

class StageViewModelTests {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var subject: StageViewModel

    @MockK private lateinit var fakeReleaseDesignConfig: ReleaseDesignConfig
    @MockK private lateinit var followManager: FollowManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { fakeReleaseDesignConfig.isReleaseDesignActive() } returns true
        every { followManager.isSelf(any()) } returns false
        subject = StageViewModel("broadcaster", fakeReleaseDesignConfig, mockk(), followManager)
    }

    @Test
    fun `showing overlays on a live stage with good quality shows avatar overlap live badge`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertTrue(subject.getAvatarOverlapLiveBadgeVisibility())
    }

    @Test
    fun `showing overlays on a live stage with poor quality shows avatar overlap live badge`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.POOR)
        subject.updateStageIsLive(true)
        assertTrue(subject.getAvatarOverlapLiveBadgeVisibility())
    }

    @Test
    fun `showing overlays on an offline stage does not show avatar overlap live badge`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        assertFalse(subject.getAvatarOverlapLiveBadgeVisibility())
    }

    @Test
    fun `showing overlays on a live release stage with good quality hides classic live indicator`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertFalse(subject.getClassicLiveIndicatorTextViewVisibility())
    }

    @Test
    fun `showing overlays on an offline stage does not show game logo`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        assertFalse(subject.getGameLogoVisibility())
    }

    @Test
    fun `showing overlays on a live stage shows game logo`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertTrue(subject.getGameLogoVisibility())
    }

    @Test
    fun `hiding overlays on an offline stage hides game logo`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        assertFalse(subject.getGameLogoVisibility())
    }

    @Test
    fun `showing overlays on an offline stage shows swipe button`() {
        subject.isReleaseDesign.set(false)
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        assertTrue(subject.getSwipeButtonVisibility())
    }

    @Test
    fun `hiding overlays hides the live indicator and avatar container`() {
        subject.updateOverlayIsVisible(false, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertFalse(subject.getLiveIndicatorAndAvatarContainerVisibility())
    }

    @Test
    fun `showing overlays shows the live indicator and avatar container`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertTrue(subject.getLiveIndicatorAndAvatarContainerVisibility())
    }

    @Test
    fun `hiding overlays hides app bar`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.updateOverlayIsVisible(false, false)
        assertFalse(subject.getAppBarVisibility())
    }

    @Test
    fun `showing overlays shows app bar if it's included`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertTrue(subject.getAppBarVisibility())
    }

    @Test
    fun `showing overlays does not show the app bar if it's not included`() {
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertFalse(subject.getAppBarVisibility())
    }

    @Test
    fun `showing overlays on a live stage shows avatar username container when user is not me`() {
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        assertTrue(subject.getAvatarUsernameContainerVisibility())
    }

    @Test
    fun `showing overlays on an offline stage shows avatar username container`() {
        subject.updateOverlayIsVisible(true, false)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        assertTrue(subject.getAvatarUsernameContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with poor quality shows weak connection container`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.POOR)
        subject.updateStageIsLive(false)
        assertTrue(subject.getWeakConnectionContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with good quality does not show weak connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        assertFalse(subject.getWeakConnectionContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with bad quality does not show weak connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.BAD)
        subject.updateStageIsLive(false)
        assertFalse(subject.getWeakConnectionContainerVisibility())
    }

    @Test
    fun `showing overlays on a live stage with bad quality shows bad connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.BAD)
        subject.updateStageIsLive(false)
        assertTrue(subject.getBadConnectionOverlayVisibility())
    }

    @Test
    fun `showing overlays on a live stage with good quality hides the bad connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        assertFalse(subject.getBadConnectionOverlayVisibility())
    }

    @Test
    fun `showing overlays on a live stage with poor quality hides the bad connection overlay`() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.POOR)
        subject.updateStageIsLive(false)
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

    @Test
    fun `show user avatar when not showing the profile section`() {
        subject.updateIsViewProfile(false)
        assertEquals(subject.getAvatarVisibility(), View.VISIBLE)
    }

    @Test
    fun `do not show user avatar when showing the profile section`() {
        subject.updateIsViewProfile(true)
        assertEquals(subject.getAvatarVisibility(), View.INVISIBLE)
    }

    @Test
    fun `show chat toggle when showing the profile section`() {
        subject.updateIsViewProfile(true)
        assertEquals(subject.getChatToggleVisibility(), View.VISIBLE)
    }

    @Test
    fun `do not show chat toggle when not showing the profile section`() {
        subject.updateIsViewProfile(false)
        assertEquals(subject.getChatToggleVisibility(), View.INVISIBLE)
    }

    @Test
    fun `show age restriction indicator if content rating is seventeen plus for release design`() {
        subject.isReleaseDesign.set(true)
        turnOnBasicIndicatorVisibility()
        subject.contentRating = ContentRating.SEVENTEEN_PLUS
        assertEquals(subject.getAgeRestrictionVisibility(), View.VISIBLE)
    }

    @Test
    fun `do not not show age restriction if overlay is not visible for release design`() {
        subject.isReleaseDesign.set(true)
        subject.updateOverlayIsVisible(false, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
        subject.contentRating = ContentRating.SEVENTEEN_PLUS
        assertEquals(subject.getAgeRestrictionVisibility(), View.GONE)
    }

    @Test
    fun `don not show age restriction if feed quality is bad for release design`() {
        subject.isReleaseDesign.set(true)
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.BAD)
        subject.updateStageIsLive(true)
        subject.contentRating = ContentRating.SEVENTEEN_PLUS
        assertEquals(subject.getAgeRestrictionVisibility(), View.GONE)
    }

    @Test
    fun `don not show age restriction if stage is not live for release design`() {
        subject.isReleaseDesign.set(true)
        subject.updateOverlayIsVisible(false, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(false)
        subject.contentRating = ContentRating.SEVENTEEN_PLUS
        assertEquals(subject.getAgeRestrictionVisibility(), View.GONE)
    }

    @Test
    fun `do not show age restriction indicator if content rating is everyone for release design`() {
        subject.isReleaseDesign.set(true)
        turnOnBasicIndicatorVisibility()
        subject.contentRating = ContentRating.EVERYONE
        assertEquals(subject.getAgeRestrictionVisibility(), View.GONE)
    }

    @Test
    fun `do not show age restriction indicator if content rating is seventeen plus for classic design`() {
        subject.isReleaseDesign.set(false)
        turnOnBasicIndicatorVisibility()
        subject.contentRating = ContentRating.SEVENTEEN_PLUS
        assertEquals(subject.getAgeRestrictionVisibility(), View.GONE)
    }

    @Test
    fun `do not show age restriction indicator if content rating is everyone for classic design`() {
        subject.isReleaseDesign.set(false)
        turnOnBasicIndicatorVisibility()
        subject.contentRating = ContentRating.EVERYONE
        assertEquals(subject.getAgeRestrictionVisibility(), View.GONE)
    }

    @Test
    fun `when release design show release version of avatar background ring`() {
        subject.isReleaseDesign.set(true)
        subject.updateAvatarImageBackground()
        assertEquals(subject.avatarImageBackground, R.drawable.circle_white_with_stage_avatar_white_rim)
    }

    @Test
    fun `when classic design show and user is fllowed show blue avatar background ring`() {
        subject.isReleaseDesign.set(false)
        subject.updateIsFollowed(true)
        subject.updateAvatarImageBackground()
        assertEquals(subject.avatarImageBackground, R.drawable.circle_white_with_blue_rim)
    }

    @Test
    fun `when classic design show and user is not fllowed show white avatar background ring`() {
        subject.isReleaseDesign.set(false)
        subject.updateIsFollowed(false)
        subject.updateAvatarImageBackground()
        assertEquals(subject.avatarImageBackground, R.drawable.circle_white)
    }

    @Test
    fun `the follow button is visible if the broadcaster is not me`() {
        every { followManager.isSelf(any()) } returns false
        assertEquals(View.VISIBLE, subject.getFollowButtonVisibility())
    }

    @Test
    fun `the follow button is gone if the broadcaster is me`() {
        every { followManager.isSelf(any()) } returns true
        assertEquals(View.GONE, subject.getFollowButtonVisibility())
    }

    private fun turnOnBasicIndicatorVisibility() {
        subject.updateOverlayIsVisible(true, true)
        subject.updateFeedQuality(FeedQuality.GOOD)
        subject.updateStageIsLive(true)
    }
}
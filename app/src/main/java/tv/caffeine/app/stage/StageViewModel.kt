package tv.caffeine.app.stage

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.databinding.Bindable
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import tv.caffeine.app.CaffeineConstants.RATING_SEVENTEEN_PLUS_TEXT
import tv.caffeine.app.R
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.stream.type.ContentRating
import tv.caffeine.app.ui.CaffeineViewModel

class StageViewModel(
    val releaseDesignConfig: ReleaseDesignConfig,
    val onAvatarButtonClick: () -> Unit
) : CaffeineViewModel() {

    private var feedQuality = FeedQuality.GOOD
    private var isMe = false
    private var overlayIsVisible = false
    private var stageIsLive = false
    private var appbarIsVisible = false
    private var isFollowed = false
    private var isViewingProfile = false

    val isReleaseDesign = ObservableBoolean(releaseDesignConfig.isReleaseDesignActive())
    var usernameTextColor = R.color.white
    var avatarImageBackground = R.drawable.circle_white

    private var _showPoorConnectionAnimation = MutableLiveData(false)
    var showPoorConnectionAnimation = _showPoorConnectionAnimation.map { it }

    var isProfileOverlayVisible = false
        set(value) {
            field = value
            notifyChange()
        }

    var contentRating = ContentRating.EVERYONE
        set(value) {
            field = value
            notifyChange()
        }

    fun getGameLogoVisibility() = overlayIsVisible && stageIsLive && feedQuality != FeedQuality.BAD

    fun getClassicLiveIndicatorTextViewVisibility() =
        !isReleaseDesign.get() &&
            getBasicIndicatorVisibility()

    fun getAvatarOverlapLiveBadgeVisibility() =
        isReleaseDesign.get() &&
            getBasicIndicatorVisibility()

    fun getAgeRestrictionVisibility() =
        isReleaseDesign.get() &&
            contentRating == ContentRating.SEVENTEEN_PLUS &&
            getBasicIndicatorVisibility()

    fun getLiveIndicatorAndAvatarContainerVisibility() = overlayIsVisible

    fun getAppBarVisibility() = overlayIsVisible && appbarIsVisible

    fun getAvatarUsernameContainerVisibility() = !isMe && overlayIsVisible && feedQuality != FeedQuality.BAD

    fun getWeakConnnectionContainerVisibility() = feedQuality == FeedQuality.POOR

    fun getBadConnectionOverlayVisibility() = feedQuality == FeedQuality.BAD

    fun getSwipeButtonVisibility() = overlayIsVisible

    fun getAgeRestriction() = if (contentRating == ContentRating.SEVENTEEN_PLUS) RATING_SEVENTEEN_PLUS_TEXT else ""

    fun updateFeedQuality(quality: FeedQuality) {
        feedQuality = quality
        updatePoorConnectionAnimation()
    }

    fun updateIsMe(isMe: Boolean) {
        this.isMe = isMe
    }

    fun updateStageIsLive(isLive: Boolean) {
        stageIsLive = isLive
    }

    fun updateOverlayIsVisible(isVisible: Boolean, shouldIncludeAppBar: Boolean) {
        overlayIsVisible = isVisible
        appbarIsVisible = shouldIncludeAppBar
        updatePoorConnectionAnimation()
    }

    fun updateIsFollowed(isFollowed: Boolean) {
        this.isFollowed = isFollowed
        updateAvatarImageBackground()
        updateUsernameTextColor()
    }

    fun updateIsViewProfile(isViewingProfile: Boolean) {
        this.isViewingProfile = isViewingProfile
        updateAvatarImageBackground()
        notifyChange()
    }

    fun onAvatarClick() = onAvatarButtonClick()

    // TODO: Remove this when move to full release design
    // We only want to have the avatar button be clickable in classic mode while in landscape
    fun onLandscapeAvatarClick() {
        if (!isReleaseDesign.get()) {
            onAvatarButtonClick()
        }
    }

    @Bindable
    fun getAvatarVisibility() = if (isViewingProfile) View.INVISIBLE else View.VISIBLE

    @Bindable
    fun getChatToggleVisibility() = if (isViewingProfile) View.VISIBLE else View.INVISIBLE

    @Bindable
    fun getProfileOverlayVisibility() = if (isProfileOverlayVisible) View.VISIBLE else View.GONE

    private fun updatePoorConnectionAnimation() {
        _showPoorConnectionAnimation.value = !overlayIsVisible && feedQuality == FeedQuality.POOR
    }

    @VisibleForTesting
    fun updateAvatarImageBackground() {
        val background: Int =
            if (isReleaseDesign.get()) {
                R.drawable.circle_white_with_stage_avatar_white_rim
            } else {
                if (isFollowed) R.drawable.circle_white_with_blue_rim else R.drawable.circle_white
            }
        avatarImageBackground = background
    }

    private fun updateUsernameTextColor() {
        usernameTextColor = if (isFollowed && !isReleaseDesign.get()) R.color.caffeine_blue else R.color.white
    }

    private fun getBasicIndicatorVisibility() =
        overlayIsVisible &&
            stageIsLive &&
            feedQuality != FeedQuality.BAD
}
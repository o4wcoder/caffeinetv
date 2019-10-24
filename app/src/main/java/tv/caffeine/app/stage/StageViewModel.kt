package tv.caffeine.app.stage

import android.view.View
import androidx.databinding.Bindable
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import tv.caffeine.app.R
import tv.caffeine.app.settings.ReleaseDesignConfig
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
    private var hasFriendsWatching = false

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

    fun getGameLogoVisibility() = overlayIsVisible && stageIsLive && feedQuality != FeedQuality.BAD

    fun getClassicLiveIndicatorTextViewVisibility() =
        !isReleaseDesign.get() &&
            overlayIsVisible &&
            stageIsLive &&
            feedQuality != FeedQuality.BAD

    fun getLiveIndicatorVisibility() =
        isReleaseDesign.get() &&
            !hasFriendsWatching &&
            overlayIsVisible &&
            stageIsLive &&
            feedQuality != FeedQuality.BAD

    fun getFriendsWatchingIndicatorVisiblility() =
        isReleaseDesign.get() &&
            hasFriendsWatching &&
            overlayIsVisible &&
            stageIsLive &&
            feedQuality != FeedQuality.BAD

    fun getLiveIndicatorAndAvatarContainerVisibility() = overlayIsVisible

    fun getAppBarVisibility() = overlayIsVisible && appbarIsVisible

    fun getAvatarUsernameContainerVisibility() = getOfflineAvatarUsernameContainerVisibility() && !isMe && overlayIsVisible && feedQuality != FeedQuality.BAD

    fun getWeakConnnectionContainerVisibility() = feedQuality == FeedQuality.POOR

    fun getBadConnectionOverlayVisibility() = feedQuality == FeedQuality.BAD

    fun getSwipeButtonVisibility() = overlayIsVisible

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

    fun updateHasFriendsWatching(hasFriendsWatching: Boolean) {
        this.hasFriendsWatching = hasFriendsWatching
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

    private fun getOfflineAvatarUsernameContainerVisibility() =
        if (isReleaseDesign.get()) {
            stageIsLive
        } else {
            true
        }

    private fun updateAvatarImageBackground() {
        val background: Int = if (isReleaseDesign.get()) {
            if (isViewingProfile) R.drawable.circle_white_with_cyan_rim else R.drawable.circle_white_with_stage_avatar_white_rim
        } else {
            if (isFollowed) R.drawable.circle_white_with_blue_rim else R.drawable.circle_white
        }
        avatarImageBackground = background
    }

    private fun updateUsernameTextColor() {
        usernameTextColor = if (isFollowed && !isReleaseDesign.get()) R.color.caffeine_blue else R.color.white
    }
}
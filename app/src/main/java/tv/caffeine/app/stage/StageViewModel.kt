package tv.caffeine.app.stage

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class StageViewModel @Inject constructor(
    releaseDesignConfig: ReleaseDesignConfig
) : CaffeineViewModel() {

    private var feedQuality = FeedQuality.GOOD
    private var isMe = false
    private var overlayIsVisible = false
    private var stageIsLive = false
    private var appbarIsVisible = false

    val isReleaseDesign = ObservableBoolean(releaseDesignConfig.isReleaseDesignActive())

    var _showPoorConnectionAnimation = MutableLiveData(false)
    var showPoorConnectionAnimation = _showPoorConnectionAnimation.map { it }

    fun getGameLogoVisibility() = overlayIsVisible && stageIsLive && feedQuality != FeedQuality.BAD

    fun getLiveIndicatorTextViewVisibility() = overlayIsVisible && stageIsLive && feedQuality != FeedQuality.BAD

    fun getLiveIndicatorAndAvatarContainerVisibility() = overlayIsVisible

    fun getAppBarVisibility() = overlayIsVisible && appbarIsVisible

    fun getAvatarUsernameContainerVisibility() = !isMe && overlayIsVisible && feedQuality != FeedQuality.BAD

    fun getWeakConnnectionContainerVisibility() = feedQuality == FeedQuality.POOR

    fun getBadConnectionOverlayVisibility() = feedQuality == FeedQuality.BAD

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

    private fun updatePoorConnectionAnimation() {
        _showPoorConnectionAnimation.value = !overlayIsVisible && feedQuality == FeedQuality.POOR
    }
}
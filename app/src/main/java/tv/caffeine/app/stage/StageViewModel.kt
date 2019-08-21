package tv.caffeine.app.stage

import android.view.View
import androidx.databinding.Bindable
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

    @Bindable
    fun getGameLogoVisibility() =
        if (overlayIsVisible && stageIsLive && feedQuality != FeedQuality.BAD) View.VISIBLE else View.GONE

    @Bindable
    fun getLiveIndicatorTextViewVisibility() =
        if (overlayIsVisible && stageIsLive && feedQuality != FeedQuality.BAD) View.VISIBLE else View.GONE

    @Bindable
    fun getLiveIndicatorAndAvatarContainerVisibility() =
        if (overlayIsVisible) View.VISIBLE else View.GONE

    @Bindable
    fun getAppBarVisibility() =
        if (overlayIsVisible && appbarIsVisible) View.VISIBLE else View.GONE

    @Bindable
    fun getAvatarUsernameContainerVisibility() =
        if (!isMe && overlayIsVisible && feedQuality != FeedQuality.BAD) View.VISIBLE else View.GONE

    @Bindable
    fun getWeakConnnectionContainerVisibility() =
        if (feedQuality == FeedQuality.POOR) View.VISIBLE else View.GONE

    @Bindable
    fun getBadConnectionOverlayVisibility() =
        if (feedQuality == FeedQuality.BAD) View.VISIBLE else View.GONE

    fun updateFeedQuality(quality: FeedQuality) {
        feedQuality = quality
        updatePoorConnectionAnimation()
        notifyChange()
    }

    fun updateIsMe(isMe: Boolean) {
        this.isMe = isMe
        notifyChange()
    }

    fun updateStageIsLive(isLive: Boolean) {
        stageIsLive = isLive
        notifyChange()
    }

    fun updateOverlayIsVisible(isVisible: Boolean, shouldIncludeAppBar: Boolean) {
        overlayIsVisible = isVisible
        appbarIsVisible = shouldIncludeAppBar
        updatePoorConnectionAnimation()
        notifyChange()
    }

    private fun updatePoorConnectionAnimation() {
        _showPoorConnectionAnimation.value = !overlayIsVisible && feedQuality == FeedQuality.POOR
    }
}
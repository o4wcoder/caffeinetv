package tv.caffeine.app.ui

import android.content.Context
import android.view.View
import androidx.databinding.Bindable
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.formatFriendsWatchingShortString
import tv.caffeine.app.lobby.release.OnlineBroadcaster

class AvatarOverlapLiveBadgeViewModel(val context: Context) : CaffeineViewModel() {

    /**
     * The text if there are friends watching
     * In lobby will equal lobbyBroadcaster.friendsWatchingText
     * On stage will be the text if friends are watching
     */
    var friendsWatchingText: String? = null

    /**
     * The text to be displayed in this component
     * In lobby will equal lobbyBroadcaster.badgeText
     * On stage will be equal to friendsWatchingText if there are followers
     **/
    @Bindable
    var titleText: String? = null

    /** The followers from which we'll get the avatar url. */
    var followers: List<User>? = null

    /** The OnlineBroadcaster associated with this component. */
    var lobbyBroadcaster: OnlineBroadcaster? = null
        set(value) {
            field = value
            titleText = lobbyBroadcaster?.badgeText
            friendsWatchingText = lobbyBroadcaster?.friendsWatchingText
            followers = lobbyBroadcaster?.broadcaster?.followingViewers
            notifyChange()
        }

    /** The friends who are following on the Stage associated with this component */
    var stageFollowers: List<User>? = null
        set(value) {
            field = value
            followers = value
            friendsWatchingText = value?.let {
                formatFriendsWatchingShortString(context, it)
            }
            titleText = friendsWatchingText
            notifyChange()
        }

    @Bindable
    fun getLiveBadgeVisibility() =
        if (titleText.isNullOrEmpty() && followers.isNullOrEmpty()) View.VISIBLE else View.GONE

    @Bindable
    fun getAvatar1Visibility() =
        if (loadOrShowAvatar1()) View.VISIBLE else View.GONE

    @Bindable
    fun getAvatar2Visibility() =
        if (loadOrShowAvatar2()) View.VISIBLE else View.GONE

    @Bindable
    fun getTitleViewVisibility() =
        if (!titleText.isNullOrEmpty()) View.VISIBLE else View.GONE

    @Bindable
    fun isTitleTextSmallMargin() = !broadcasterHasServerText() // title text needs to be larger when no avatars shown

    @Bindable
    fun getAvatar1Url() = if (loadOrShowAvatar1()) followers?.getOrNull(0)?.avatarImageUrl else null // don't load avatar if not visible

    @Bindable
    fun getAvatar2Url() = if (loadOrShowAvatar2()) followers?.getOrNull(1)?.avatarImageUrl else null // don't load avatar if not visible

    /** If an OnlineBroadcaster has server text it will be different from the friends watching text and should override all other text */
    private fun broadcasterHasServerText() = titleText != friendsWatchingText
    private fun loadOrShowAvatar1() = !titleText.isNullOrEmpty() && !broadcasterHasServerText()
    private fun loadOrShowAvatar2() = !titleText.isNullOrEmpty() && !broadcasterHasServerText() && followers?.getOrNull(1) != null
}
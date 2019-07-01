package tv.caffeine.app.lobby.release

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.navigation.NavDirections
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.analytics.LobbyImpressionAnalytics
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.LobbySwipeFragmentDirections
import tv.caffeine.app.lobby.formatFriendsWatchingShortString
import tv.caffeine.app.session.FollowManager

sealed class NavigationCommand {
    data class To(val directions: NavDirections) : NavigationCommand()
}

abstract class AbstractBroadcaster(
    val followManager: FollowManager,
    val broadcaster: Lobby.Broadcaster,
    val lobbyImpressionAnalytics: LobbyImpressionAnalytics,
    val coroutineScope: CoroutineScope
) {

    private val user = broadcaster.user
    val username = user.username
    protected val caid = user.caid

    private val _navigationCommands = MutableLiveData<Event<NavigationCommand>>()
    val navigationCommands = _navigationCommands.map { it }

    val isFollowing = MutableLiveData(followManager.isFollowing(caid))
    val userIcon = when {
        user.isVerified -> R.drawable.verified
        user.isCaster -> R.drawable.caster
        else -> 0
    }
    val avatarImageUrl = user.avatarImageUrl

    protected fun navigate(action: NavDirections) {
        val navigationCommand = NavigationCommand.To(action)
        _navigationCommands.value = Event(navigationCommand)
    }

    fun cardClicked() {
        coroutineScope.launch {
            lobbyImpressionAnalytics.cardClicked(broadcaster)
        }
        val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment(username)
        navigate(action)
    }

    fun followClicked() = coroutineScope.launch {
        if (followManager.isFollowing(caid)) {
            val action = MainNavDirections.actionGlobalUnfollowUserDialogFragment(username)
            navigate(action)
        } else {
            followManager.followUser(caid)
            lobbyImpressionAnalytics.followClicked(broadcaster)
        }
        withContext(Dispatchers.Main) {
            isFollowing.value = followManager.isFollowing(caid)
        }
    }
}

class OnlineBroadcaster @AssistedInject constructor (
    context: Context,
    followManager: FollowManager,
    @Assisted broadcaster: Lobby.Broadcaster,
    @Assisted lobbyImpressionAnalytics: LobbyImpressionAnalytics,
    @Assisted coroutineScope: CoroutineScope
) : AbstractBroadcaster(followManager, broadcaster, lobbyImpressionAnalytics, coroutineScope) {
    @AssistedInject.Factory
    interface Factory {
        fun create(
            broadcaster: Lobby.Broadcaster,
            lobbyImpressionAnalytics: LobbyImpressionAnalytics,
            coroutineScope: CoroutineScope
        ): OnlineBroadcaster
    }

    // TODO in the future, this should be called when the card is actually visible
    init {
        coroutineScope.launch {
            lobbyImpressionAnalytics.sendImpressionEventData(broadcaster)
        }
    }

    private val broadcast = broadcaster.broadcast
    val broadcastTitle = broadcast?.name
    val mainPreviewImageUrl = broadcast?.mainPreviewImageUrl
    val pictureInPictureImageUrl = broadcast?.pictureInPictureImageUrl
    val friendsWatchingText = formatFriendsWatchingShortString(context, broadcaster) ?: context.getString(R.string.live_indicator)

    val contentRating = if (broadcast?.name?.startsWith("[17+]") == true) "17+" else null
    fun kebabClicked() {
        val action = MainNavDirections.actionGlobalReportOrIgnoreDialogFragment(caid, username, false)
        navigate(action)
    }
}

class OfflineBroadcaster @AssistedInject constructor(
    followManager: FollowManager,
    @Assisted broadcaster: Lobby.Broadcaster,
    @Assisted lobbyImpressionAnalytics: LobbyImpressionAnalytics,
    @Assisted coroutineScope: CoroutineScope
) : AbstractBroadcaster(followManager, broadcaster, lobbyImpressionAnalytics, coroutineScope) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            broadcaster: Lobby.Broadcaster,
            lobbyImpressionAnalytics: LobbyImpressionAnalytics,
            coroutineScope: CoroutineScope
        ): OfflineBroadcaster
    }
}

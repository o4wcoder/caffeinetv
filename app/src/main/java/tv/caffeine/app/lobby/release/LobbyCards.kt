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
import tv.caffeine.app.lobby.LiveBroadcast
import tv.caffeine.app.lobby.LobbySwipeFragmentDirections
import tv.caffeine.app.lobby.formatFriendsWatchingString
import tv.caffeine.app.session.FollowManager

sealed class NavigationCommand {
    data class To(val directions: NavDirections) : NavigationCommand()
}

sealed class LobbyCard

class OnlineBroadcaster @AssistedInject constructor (
    context: Context,
    private val followManager: FollowManager,
    @Assisted val liveBroadcast: LiveBroadcast,
    @Assisted private val lobbyImpressionAnalytics: LobbyImpressionAnalytics,
    @Assisted private val coroutineScope: CoroutineScope
) : LobbyCard() {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            liveBroadcast: LiveBroadcast,
            lobbyImpressionAnalytics: LobbyImpressionAnalytics,
            coroutineScope: CoroutineScope
        ): OnlineBroadcaster
    }

    // TODO in the future, this should be called when the card is actually visible
    init {
        coroutineScope.launch {
            lobbyImpressionAnalytics.sendImpressionEventData(liveBroadcast)
        }
    }

    private val user = liveBroadcast.broadcaster.user
    private val broadcast = liveBroadcast.broadcaster.broadcast
    val username = user.username
    private val caid = user.caid

    private val _navigationCommands = MutableLiveData<Event<NavigationCommand>>()
    val navigationCommands = _navigationCommands.map { it }

    val isFollowing = MutableLiveData(followManager.isFollowing(caid))
    val broadcastTitle = broadcast?.name
    val userIcon = when {
        user.isVerified -> R.drawable.verified
        user.isCaster -> R.drawable.caster
        else -> 0
    }
    val avatarImageUrl = user.avatarImageUrl
    val mainPreviewImageUrl = broadcast?.mainPreviewImageUrl
    val pictureInPictureImageUrl = broadcast?.pictureInPictureImageUrl
    val friendsWatchingText = formatFriendsWatchingString(context, liveBroadcast.broadcaster)
    val contentRating = if (broadcast?.name?.startsWith("[17+]") == true) "17+" else null

    fun followClicked() = coroutineScope.launch {
        if (followManager.isFollowing(caid)) {
            val action = MainNavDirections.actionGlobalUnfollowUserDialogFragment(username)
            navigate(action)
        } else {
            followManager.followUser(caid)
            lobbyImpressionAnalytics.followClicked(liveBroadcast)
        }
        withContext(Dispatchers.Main) {
            isFollowing.value = followManager.isFollowing(caid)
        }
    }

    fun kebabClicked() {
        val action = MainNavDirections.actionGlobalReportOrIgnoreDialogFragment(caid, username, false)
        navigate(action)
    }

    fun cardClicked() {
        coroutineScope.launch {
            lobbyImpressionAnalytics.cardClicked(liveBroadcast)
        }
        val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment(username)
        navigate(action)
    }

    private fun navigate(action: NavDirections) {
        val navigationCommand = NavigationCommand.To(action)
        _navigationCommands.value = Event(navigationCommand)
    }
}

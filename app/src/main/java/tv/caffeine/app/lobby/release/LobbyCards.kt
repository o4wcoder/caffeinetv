package tv.caffeine.app.lobby.release

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
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
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.formatFriendsWatchingShortString
import tv.caffeine.app.lobby.fragment.ClusterData
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.session.FollowManager

sealed class NavigationCommand {
    data class To(val directions: NavDirections) : NavigationCommand()
}

abstract class AbstractBroadcaster(
    val followManager: FollowManager,
    val user: User,
    val coroutineScope: CoroutineScope
) {

    val username = user.username
    protected val caid = user.caid

    private val _navigationCommands = MutableLiveData<Event<NavigationCommand>>()
    val navigationCommands = _navigationCommands.map { it }

    val isFollowing = MutableLiveData(followManager.isFollowing(caid))
    val isSelf = followManager.isSelf(caid)
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

    open fun followClicked() {
        coroutineScope.launch {
            if (followManager.isFollowing(caid)) {
                followManager.unfollowUser(caid)
            } else {
                followManager.followUser(caid)
            }
            withContext(Dispatchers.Main) {
                isFollowing.value = followManager.isFollowing(caid)
            }
        }
    }

    open fun cardClicked() {
    }

    open fun userClicked() {
    }
}

class FPGBroadcaster(
    followManager: FollowManager,
    user: User,
    coroutineScope: CoroutineScope
) : AbstractBroadcaster(followManager, user, coroutineScope) {

    override fun userClicked() {
        val action = MainNavDirections.actionGlobalStagePagerFragment(user.username)
        navigate(action)
    }
}

abstract class AbstractLobbyBroadcaster(
    followManager: FollowManager,
    val broadcaster: Lobby.Broadcaster,
    private val lobbyImpressionAnalytics: LobbyImpressionAnalytics,
    coroutineScope: CoroutineScope,
    private val allDistinctLiveBroadcasters: List<String>? = null
) : AbstractBroadcaster(followManager, broadcaster.user, coroutineScope) {

    override fun cardClicked() {
        coroutineScope.launch {
            lobbyImpressionAnalytics.cardClicked(broadcaster)
        }
        val action = MainNavDirections.actionGlobalStagePagerFragment(
            username, allDistinctLiveBroadcasters?.toTypedArray())
        navigate(action)
    }

    override fun followClicked() {
        super.followClicked()
        coroutineScope.launch {
            lobbyImpressionAnalytics.followClicked(broadcaster)
        }
    }
}

class OnlineBroadcaster @AssistedInject constructor (
    context: Context,
    followManager: FollowManager,
    @Assisted broadcaster: Lobby.Broadcaster,
    @Assisted lobbyImpressionAnalytics: LobbyImpressionAnalytics,
    @Assisted coroutineScope: CoroutineScope,
    @Assisted allDistinctLiveBroadcasters: List<String>? = null
) : AbstractLobbyBroadcaster(
    followManager, broadcaster, lobbyImpressionAnalytics, coroutineScope, allDistinctLiveBroadcasters
) {
    @AssistedInject.Factory
    interface Factory {
        fun create(
            broadcaster: Lobby.Broadcaster,
            lobbyImpressionAnalytics: LobbyImpressionAnalytics,
            coroutineScope: CoroutineScope,
            allDistinctLiveBroadcasters: List<String>?
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
    private val friendsWatchingText = formatFriendsWatchingShortString(context, broadcaster)
        ?: context.getString(R.string.live_indicator_lowercase)
    val badgeText = broadcaster.badgeText?.let { it } ?: friendsWatchingText
    val ageRestriction = broadcaster.ageRestriction
    val ageRestrictionVisibility = if (ageRestriction != null) View.VISIBLE else View.GONE

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
) : AbstractLobbyBroadcaster(followManager, broadcaster, lobbyImpressionAnalytics, coroutineScope) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            broadcaster: Lobby.Broadcaster,
            lobbyImpressionAnalytics: LobbyImpressionAnalytics,
            coroutineScope: CoroutineScope
        ): OfflineBroadcaster
    }
}

class CategoryCardViewModel @AssistedInject constructor(
    @Assisted categoryCard: ClusterData.CategoryCard,
    @Assisted context: Context,
    serverConfig: ServerConfig
) {

    @AssistedInject.Factory
    interface Factory {
        fun create(categoryCard: ClusterData.CategoryCard, context: Context): CategoryCardViewModel
    }

    private var isNameVisible = categoryCard.overlayImagePath == null
    private val cardId = categoryCard.id

    private val _navigationCommands = MutableLiveData<Event<NavigationCommand>>()
    val navigationCommands = _navigationCommands.map { it }

    val name = categoryCard.name
    val nameVisibility = if (isNameVisible) View.VISIBLE else View.GONE
    val gradientDrawable = if (isNameVisible) ContextCompat.getDrawable(context, R.drawable.gradient_overlay) else null
    val backgroundImageUrl = categoryCard.backgroundImagePath?.let { "${serverConfig.images}$it" }
    val overlayImageUrl = categoryCard.overlayImagePath?.let { "${serverConfig.images}$it" }

    fun cardClicked() {
        val action = MainNavDirections.actionGlobalLobbyDetailFragment(cardId, name)
        _navigationCommands.value = Event(NavigationCommand.To(action))
    }
}

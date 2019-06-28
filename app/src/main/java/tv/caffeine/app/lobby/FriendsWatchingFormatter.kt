package tv.caffeine.app.lobby

import android.content.Context
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby

fun formatFriendsWatchingString(context: Context, broadcaster: Lobby.Broadcaster): String? {
    val firstFriendVerified = broadcaster.followingViewers?.firstOrNull()?.isVerified == true
    val firstFriendIsCaster = broadcaster.followingViewers?.firstOrNull()?.isCaster == true
    val singleFriendWatchingResId = when {
        firstFriendVerified -> R.string.verified_user_watching
        firstFriendIsCaster -> R.string.caster_watching
        else -> R.string.user_watching
    }
    val multipleFriendsWatchingResId = when {
        firstFriendVerified -> R.plurals.verified_user_and_friends_watching
        firstFriendIsCaster -> R.plurals.caster_and_friends_watching
        else -> R.plurals.user_and_friends_watching
    }
    return when (broadcaster.followingViewersCount) {
        0 -> null
        1 -> {
            broadcaster.followingViewers?.let {
                context.getString(
                    singleFriendWatchingResId,
                    broadcaster.followingViewers[0].username,
                    broadcaster.followingViewers[0].avatarImageUrl
                )
            }
        }
        else -> {
            broadcaster.followingViewers?.let {
                context.resources.getQuantityString(
                    multipleFriendsWatchingResId,
                    broadcaster.followingViewersCount - 1,
                    broadcaster.followingViewers[0].username,
                    broadcaster.followingViewers[0].avatarImageUrl,
                    broadcaster.followingViewersCount - 1
                )
            }
        }
    }
}

package tv.caffeine.app.lobby

import android.content.Context
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User

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

fun formatFriendsWatchingShortString(context: Context, broadcaster: Lobby.Broadcaster): String? =
    broadcaster.followingViewers?.let {
        formatFriendsWatchingShortString(context, it, broadcaster.followingViewersCount)
    }

fun formatFriendsWatchingShortString(
    context: Context,
    followingViewers: List<User>,
    followingViewersCount: Int = followingViewers.size
): String? {
    val singleFriendWatchingResId = R.string.user_watching_short
    val multipleFriendsWatchingResId = R.plurals.user_and_friends_watching_short
    return when (followingViewersCount) {
        0 -> null
        1 -> {
            followingViewers.let {
                context.getString(
                    singleFriendWatchingResId,
                    followingViewers[0].username,
                    followingViewers[0].avatarImageUrl
                )
            }
        }
        else -> {
            followingViewers.let {
                context.resources.getQuantityString(
                    multipleFriendsWatchingResId,
                    followingViewersCount - 1,
                    followingViewers[0].username,
                    followingViewers[0].avatarImageUrl,
                    followingViewersCount - 1
                )
            }
        }
    }
}

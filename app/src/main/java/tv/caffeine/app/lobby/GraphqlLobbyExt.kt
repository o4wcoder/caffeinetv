package tv.caffeine.app.lobby

import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.Game
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.fragment.UserFragment

fun LobbyQuery.LiveBroadcastCard.toLiveCard(): SingleCard {
    val graphqlBroadcast = broadcast
    val graphqlUser = user.fragments.userFragment
    val broadcast = Broadcast(
        graphqlBroadcast.id,
        graphqlBroadcast.name,
        graphqlBroadcast.contentId ?: "",
        graphqlBroadcast.gameImagePath?.let { Game(it) },
        null,
        graphqlBroadcast.previewImagePath,
        Broadcast.State.ONLINE,
        "", // TODO: clean up dateText
        null,
        null
    )
    val broadcaster = Lobby.Broadcaster(
        graphqlUser.caid,
        "", // TODO: clean up type
        graphqlUser.toCaffeineUser(),
        "",
        broadcast,
        null,
        graphqlBroadcast.friendViewers.map { it.fragments.userFragment.toCaffeineUser() },
        graphqlBroadcast.totalFriendViewers,
        displayOrder,
        id,
        name
    )
    return LiveBroadcast(broadcaster.id, broadcaster)
}

fun LobbyQuery.CreatorCard.toOfflineCard(): SingleCard {
    val graphqlUser = user.fragments.userFragment
    val broadcaster = Lobby.Broadcaster(
        graphqlUser.caid,
        "", // TODO: clean up type
        graphqlUser.toCaffeineUser(),
        "",
        null,
        null,
        null,
        0,
        displayOrder,
        id,
        null
    )
    return PreviousBroadcast(broadcaster.id, broadcaster)
}

fun UserFragment.toCaffeineUser() = User(
    caid,
    username,
    null,
    null,
    avatarImagePath ?: "",
    0,
    0,
    isVerified,
    isCaster,
    null,
    "",
    mapOf(), // TODO: clean up abilities
    null,
    null,
    null,
    null,
    null,
    null,
    false, // TODO: clean up isFeatured
    true,
    null,
    null,
    null,
    null
    // TODO: add isFollowing
)

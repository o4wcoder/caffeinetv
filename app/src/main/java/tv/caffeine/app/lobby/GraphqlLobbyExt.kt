package tv.caffeine.app.lobby

import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.Game
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.fragment.ClusterData
import tv.caffeine.app.lobby.fragment.UserData
import tv.caffeine.app.lobby.type.AgeRestriction

fun ClusterData.LiveBroadcastCard.toLiveCard(): LiveBroadcast {
    val graphqlBroadcast = broadcast
    val graphqlUser = user.fragments.userData
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
        graphqlBroadcast.friendViewers.map { it.fragments.userData.toCaffeineUser() },
        graphqlBroadcast.totalFriendViewers,
        displayOrder,
        id,
        name,
        graphqlBroadcast.getAgeRestrictionText()
    )
    return LiveBroadcast(broadcaster.id, broadcaster)
}

fun ClusterData.CreatorCard.toOfflineCard(): PreviousBroadcast {
    val graphqlUser = user.fragments.userData
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
        null,
        null
    )
    return PreviousBroadcast(broadcaster.id, broadcaster)
}

fun UserData.toCaffeineUser() = User(
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
    null,
    true,
    null,
    null,
    null,
    null
    // TODO: add isFollowing
)

private fun ClusterData.Broadcast.getAgeRestrictionText() =
    if (ageRestriction == AgeRestriction.SEVENTEEN_PLUS) "17+" else null

fun List<LobbyItem>.toDistinctLiveBroadcasters(): List<String> {
    val broadcasters = ArrayList<String>()
    for (item in this) {
        if (item is LiveBroadcast) {
            broadcasters.add(item.broadcaster.user.username)
        } else if (item is CardList) {
            broadcasters.addAll(item.cards.map { it.broadcaster.user.username })
        }
    }
    return broadcasters.distinct()
}

fun LobbyQuery.PagePayload.toDistinctLiveBroadcasters(): List<String> =
    clusters.flatMap { it.fragments.clusterData.cardLists }
        .filter { it.inlineFragment is ClusterData.AsLiveBroadcastCardList }
        .flatMap { (it.inlineFragment as ClusterData.AsLiveBroadcastCardList).liveBroadcastCards }
        .map { it.user.fragments.userData.username }
        .distinct()

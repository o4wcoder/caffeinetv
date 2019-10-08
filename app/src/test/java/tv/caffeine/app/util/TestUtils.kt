package tv.caffeine.app.util

import kotlinx.coroutines.Dispatchers
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.Game
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User

val TestDispatchConfig = DispatchConfig(Dispatchers.Unconfined, Dispatchers.Unconfined)

fun makeGenericUser() = User("caid", "username", "name", "email",
    "/avatarImagePath", 0, 0, false, false,
    "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode",
    "countryName", "gender", false, false, null,
    null, false, false)

fun makeOnlineBroadcast(game: Game? = null) = Broadcast("id", "name", "contentId", game,
    null, "pip", Broadcast.State.ONLINE, "date", null, null)

fun makeOfflineBroadcast(game: Game? = null) = Broadcast("id", "name", "contentId", game,
    null, "pip", Broadcast.State.OFFLINE, "date", null, null)

fun makeGame() = Game("/iconImagePath")

fun makeBroadcaster(user: User, broadcast: Broadcast?, badgeText: String? = null, ageRestriction: String? = null, followingViewers: List<User> = listOf()) =
    Lobby.Broadcaster("broadcastId", "OnlineBroadcaster", user, "tag", broadcast, null, followingViewers, followingViewers.size, 0,
        "clusterId", badgeText, ageRestriction)

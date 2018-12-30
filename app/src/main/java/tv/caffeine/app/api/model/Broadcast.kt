package tv.caffeine.app.api.model

import tv.caffeine.app.di.IMAGES_BASE_URL

class Broadcast(
        val id: String,
        val name: String,
        val contentId: String,
        val game: Game?,
        val liveHostedBroadcaster: Lobby.Broadcaster?,
        private val previewImagePath: String,
        val state: State,
        val dateText: String
) {
    val hasLiveHostedBroadcaster get() = liveHostedBroadcaster != null
    val previewImageUrl get() = "$IMAGES_BASE_URL$previewImagePath"
    val mainPreviewImageUrl: String get() = liveHostedBroadcaster?.broadcast?.previewImageUrl ?: previewImageUrl
    val pictureInPictureImageUrl get() = if (hasLiveHostedBroadcaster) previewImageUrl else null

    enum class State {
        ONLINE, OFFLINE
    }
}

fun Broadcast.isOnline() = state == Broadcast.State.ONLINE

class Game(val iconImagePath: String?)
val Game.iconImageUrl get() = "$IMAGES_BASE_URL$iconImagePath"

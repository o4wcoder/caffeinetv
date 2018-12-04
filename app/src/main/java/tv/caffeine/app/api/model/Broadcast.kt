package tv.caffeine.app.api.model

import tv.caffeine.app.di.IMAGES_BASE_URL

class Broadcast(val id: String,
                val name: String,
                val contentId: String,
                val game: Game?,
                val previewImagePath: String,
                val state: State,
                val dateText: String) {
    val previewImageUrl get() = "$IMAGES_BASE_URL$previewImagePath"

    enum class State {
        ONLINE, OFFLINE
    }
}

fun Broadcast.isOnline() = state == Broadcast.State.ONLINE

class Game(val iconImagePath: String?)
val Game.iconImageUrl get() = "$IMAGES_BASE_URL$iconImagePath"

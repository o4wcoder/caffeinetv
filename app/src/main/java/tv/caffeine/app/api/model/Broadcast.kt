package tv.caffeine.app.api.model

class Broadcast(val id: String,
                val name: String,
                val contentId: String,
                val previewImagePath: String,
                val dateText: String) {
    val previewImageUrl get() = "https://images.caffeine.tv$previewImagePath"
}
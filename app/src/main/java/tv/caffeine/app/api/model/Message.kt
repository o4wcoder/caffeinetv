package tv.caffeine.app.api.model

data class Message(val publisher: User, val id: String, val type: Type, val body: Body, val endorsementCount: Int = 0) {
    enum class Type { reaction, rescind, presence }
    data class Body(val text: String, val digitalItem: ReceivedDigitalItem? = null, val variant: Variant? = null)
    data class ReceivedDigitalItem(val id: String, val count: Int, val creditsPerItem: Int, val staticImagePath: String, val sceneKitPath: String, val webAssetPath: String) {
        val staticImageUrl get() = "https://assets.caffeine.tv$staticImagePath"
    }
    data class Variant(val background: Background, val text: Text, val theme: String, val thumbnailPathComponents: Map<String, String>, val tintColor: String, val variant: String)
    data class Background(val image: Image)
    data class Text(val alignment: Alignment, val color: String, val font: Font, val opacity: Int, val padding: Rect, val pointSize: Int)
    data class Image(val capInsets: Rect, val pathComponent: String)
    data class Rect(val top: Int, val bottom: Int, val left: Int, val right: Int)
    data class Alignment(val horizontal: Horizontal, val vertical: Vertical) {
        enum class Horizontal { center, left, right }
        enum class Vertical { middle, top, bottom }
    }
    data class Font(val customFace: String)
}


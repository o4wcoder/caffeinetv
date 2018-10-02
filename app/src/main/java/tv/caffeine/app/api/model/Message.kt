package tv.caffeine.app.api.model

data class Message(val publisher: User, val id: String, val type: String, val body: Body, val endorsementCount: Int = 0) {
    data class Body(val text: String, val digitalItem: ReceivedDigitalItem?)
    data class ReceivedDigitalItem(val id: String, val count: Int, val creditsPerItem: Int, val staticImagePath: String, val sceneKitPath: String, val webAssetPath: String) {
        val staticImageUrl get() = "https://assets.caffeine.tv$staticImagePath"
    }
}


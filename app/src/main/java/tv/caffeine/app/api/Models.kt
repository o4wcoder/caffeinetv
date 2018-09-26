package tv.caffeine.app.api

class Api {

    class Broadcast(val id: String,
                    val name: String,
                    val contentId: String,
                    val previewImagePath: String,
                    val dateText: String) {
        val previewImageUrl get() = "https://images.caffeine.tv$previewImagePath"
    }

    data class User(val caid: String,
               val username: String,
               val name: String?,
               val avatarImagePath: String,
               val followingCount: Int,
               val followersCount: Int,
               val isVerified: Boolean,
               val broadcastId: String,
               val stageId: String,
               val abilities: Map<String, Boolean>,
               val age: Any?,
               val bio: String,
               val countryCode: Any?,
               val countryName: Any?,
               val gender: Any?,
               val isFeatured: Boolean,
               val isOnline: Boolean) {
        val avatarImageUrl get() = "https://images.caffeine.tv$avatarImagePath"
    }

    class UserContainer(val user: User)

    class Game(val bannerImagePath: String,
               val description: String,
               val executableName: String?,
               val iconImagePath: String,
               val id: Int,
               val isCaptureSoftware: Boolean,
               val name: String,
               val processNames: Array<String>,
               val supported: Boolean,
               val website: String,
               val windowTitle: String?)


    class v3 {
        class Lobby {
            class Result(val tags: Map<String, Tag>, val content: Map<String, Content>, val header: Any, val sections: Array<Section>)
            class Tag(val id: String, val name: String, val color: String)
            class Content(val id: String, val type: String, val name: String, val iconImagePath: String, val bannerImagePath: String) {
                val iconImageUrl get() = "https://images.caffeine.tv$iconImagePath"
            }
            class Section(val id: String, val type: String, val name: String, val emptyMessage: String?, val broadcasters: Array<Broadcaster>?, val categories: Array<Category>?)
            class Broadcaster(val id: String, val type: String, val user: User, val tagId: String,
                              val broadcast: Broadcast?,
                              val lastBroadcast: Broadcast?,
                              val followingViewers: Array<User>,
                              val followingViewersCount: Int)
            class Category(val id: String, val name: String, val broadcasters: Array<Broadcaster>)
        }
    }

    data class Message(val publisher: User, val id: String, val type: String, val body: Body, val endorsementCount: Int = 0)
    data class Body(val text: String, val digitalItem: ReceivedDigitalItem?)
    data class ReceivedDigitalItem(val id: String, val count: Int, val creditsPerItem: Int, val staticImagePath: String, val sceneKitPath: String, val webAssetPath: String) {
        val staticImageUrl get() = "https://assets.caffeine.tv$staticImagePath"
    }

}

class SignedUserToken(val token: String)

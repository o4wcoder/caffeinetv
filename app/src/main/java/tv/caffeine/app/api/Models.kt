package tv.caffeine.app.api

class Api {

    class Model(val id: String, val name: String)

    class LobbyResult(val requestAvatarUpdate: Boolean, val welcomeCard: Boolean, val cards: Array<LobbyCard>)

    class LobbyCard(val id: String,
                    val broadcast: Broadcast,
                    val followingViewers: Array<User>,
                    val followingViewersCount: Int,
                    val score: Int,
                    val reason: String)

    class Broadcast(val id: String,
                    val name: String,
                    val gameImagePath: String?,
                    val webcamImagePath: String?,
                    val previewImagePath: String,
                    val user: User,
                    val game: Game?)

    class User(val caid: String,
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
               val isOnline: Boolean)

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
            class Content(val id: String, val type: String, val name: String, val iconImagePath: String, val bannerImagePath: String)
            class Section(val id: String, val type: String, val name: String, val emptyMessage: String?, val broadcasters: Array<Broadcaster>, val categories: Array<Category>)
            class Broadcaster(val id: String, val type: String, val user: User, val tagId: String,
                              val broadcast: Broadcast,
                              val followingViewers: Array<User>,
                              val followingCount: Int)
            class Category(val id: String, val name: String, val broadcasters: Array<Broadcaster>)
        }
    }

}

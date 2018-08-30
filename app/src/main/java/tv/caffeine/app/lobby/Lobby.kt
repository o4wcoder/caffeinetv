package tv.caffeine.app.lobby

import retrofit2.Call
import retrofit2.http.GET

interface Lobby {
    @GET("v2/lobby")
    fun lobby(): Call<LobbyResult>
}

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


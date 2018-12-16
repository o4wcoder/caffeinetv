package tv.caffeine.app.api.model

import org.threeten.bp.ZonedDateTime
import tv.caffeine.app.di.IMAGES_BASE_URL

data class User(
        val caid: String,
        val username: String,
        val name: String?,
        val email: String?,
        val avatarImagePath: String,
        val followingCount: Int,
        val followersCount: Int,
        val isVerified: Boolean,
        val broadcastId: String?,
        val stageId: String,
        val abilities: Map<String, Boolean>,
        val connectedAccounts: Map<String, ConnectedAccount>?,
        val age: Any?,
        val bio: String,
        val countryCode: Any?,
        val countryName: Any?,
        val gender: Any?,
        val isFeatured: Boolean,
        val isOnline: Boolean,
        val notificationsLastViewedAt: ZonedDateTime?,
        val mfaMethod: MfaMethod?
) {
    val avatarImageUrl get() = "$IMAGES_BASE_URL$avatarImagePath"
}

class UserContainer(val user: User)

class UserUpdateBody(val user: UserUpdateDetails)
class UserUpdateDetails(val name: String?, val bio: String?, val twitterAutoPostOnline: Boolean?)

enum class IdentityProvider {
    facebook, twitter
}

sealed class CaidRecord(val caid: String) {
    class FriendWatching(caid: String) : CaidRecord(caid)
    class FollowRecord(caid: String, val followedAt: ZonedDateTime?) : CaidRecord(caid)
    class IgnoreRecord(caid: String, val ignoredAt: ZonedDateTime?) : CaidRecord(caid)
}

class ConnectedAccount(val uid: String, val provider: IdentityProvider, val displayName: String)

enum class MfaMethod {
    NONE, EMAIL, TOTP
}

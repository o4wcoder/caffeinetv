package tv.caffeine.app.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime
import tv.caffeine.app.di.IMAGES_BASE_URL

typealias CAID = String

fun String.isCAID() = length == 36 && startsWith("CAID")

data class User(
    val caid: CAID,
    val username: String,
    val name: String?,
    val email: String?,
    val avatarImagePath: String,
    val followingCount: Int,
    val followersCount: Int,
    val isVerified: Boolean,
    val isCaster: Boolean = true,
    val broadcastId: String?,
    val stageId: String,
    val abilities: Map<String, Boolean>,
    val connectedAccounts: Map<String, ConnectedAccount>?,
    val age: Any?,
    val bio: String?,
    val countryCode: Any?,
    val countryName: Any?,
    val gender: Any?,
    val isFeatured: Boolean,
    val isOnline: Boolean,
    val notificationsLastViewedAt: ZonedDateTime?,
    val mfaMethod: MfaMethod?,
    val emailVerified: Boolean?,
    val isBroadcasting: Boolean?
) {
    val avatarImageUrl get() = "$IMAGES_BASE_URL$avatarImagePath"

    fun isMfaEnabled() = mfaMethod != MfaMethod.NONE
}

class UserContainer(val user: User)

class UserUpdateBody(val user: UserUpdateDetails)
class UserUpdateDetails(val name: String?, val bio: String?, val twitterAutoPostOnline: Boolean?)

@Parcelize
enum class IdentityProvider : Parcelable {
    facebook, twitter
}

class PaginatedFollowers(
    val limit: Int? = 100,
    val offset: Int? = 0,
    val followers: List<CaidRecord.FollowRecord>
)

class PaginatedFollowing(
    val limit: Int? = 100,
    val offset: Int? = 0,
    val following: List<CaidRecord.FollowRecord>
)

sealed class CaidRecord(val caid: CAID) {
    class FriendWatching(caid: CAID) : CaidRecord(caid)
    class FollowRecord(caid: CAID, val followedAt: ZonedDateTime?) : CaidRecord(caid)
    class IgnoreRecord(caid: CAID, val ignoredAt: ZonedDateTime?) : CaidRecord(caid)
}

class ConnectedAccount(val uid: String, val provider: IdentityProvider, val displayName: String)

enum class MfaMethod {
    NONE, EMAIL, TOTP
}

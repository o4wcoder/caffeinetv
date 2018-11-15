package tv.caffeine.app.api.model

data class User(val caid: String,
                val username: String,
                val name: String?,
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
                val isOnline: Boolean) {
    val avatarImageUrl get() = "https://images.caffeine.tv$avatarImagePath"
}

class UserContainer(val user: User)

class UserUpdateBody(val user: UserUpdateDetails)
class UserUpdateDetails(val name: String?, val bio: String?, val twitterAutoPostOnline: Boolean?)

enum class IdentityProvider {
    facebook, twitter
}

sealed class CaidRecord(val caid: String) {
    class FriendWatching(caid: String) : CaidRecord(caid)
    class FollowRecord(caid: String, val followedAt: String?) : CaidRecord(caid) // followedAt = ISO-8601 date
    class IgnoreRecord(caid: String, val ignoredAt: String?) : CaidRecord(caid) // followedAt = ISO-8601 date
}

class ConnectedAccount(val uid: String, val provider: IdentityProvider, val displayName: String)

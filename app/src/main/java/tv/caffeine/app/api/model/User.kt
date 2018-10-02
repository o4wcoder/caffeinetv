package tv.caffeine.app.api.model

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

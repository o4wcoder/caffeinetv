package tv.caffeine.app.profile

import tv.caffeine.app.api.model.MfaMethod

data class UserProfile(
        val username: String,
        val name: String?,
        val email: String?,
        val emailVerified: Boolean?,
        val followersCount: String,
        val followingCount: String,
        val bio: String?,
        val isFollowed: Boolean,
        val isVerified: Boolean,
        val avatarImageUrl: String,
        val mfaMethod: MfaMethod?,
        val stageImageUrl: String?,
        val isLive: Boolean
)

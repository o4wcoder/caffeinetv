package tv.caffeine.app.profile

import tv.caffeine.app.R
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.MfaMethod
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.iconImageUrl
import tv.caffeine.app.api.model.isOnline
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.compactThousandsNumberFormat
import tv.caffeine.app.util.longFormThousandsNumberFormat

data class UserProfile(
    val caid: CAID,
    val username: String,
    val name: String?,
    val email: String?,
    val emailVerified: Boolean?,
    private val followersCount: Int,
    private val followingCount: Int,
    val bio: String?,
    val isFollowed: Boolean,
    val isVerified: Boolean,
    val isCaster: Boolean,
    val avatarImageUrl: String,
    val mfaMethod: MfaMethod?,
    val stageImageUrl: String?,
    val isLive: Boolean,
    val isMe: Boolean,
    val shouldShowFollow: Boolean = !isFollowed && !isMe,
    val twitterUsername: String? = null,
    val broadcastName: String? = null,
    val gameIconImageUrl: String? = null
) {

    constructor(
        user: User,
        broadcastDetails: Broadcast?,
        followManager: FollowManager
    ) : this(
            user.caid,
            user.username,
            user.name,
            user.email,
            user.emailVerified,
            user.followersCount,
            user.followingCount,
            user.bio,
            followManager.isFollowing(user.caid),
            user.isVerified,
            user.isCaster,
            user.avatarImageUrl,
            user.mfaMethod,
            stageImageUrl = if (broadcastDetails?.isOnline() == true) broadcastDetails.mainPreviewImageUrl else null,
            isLive = broadcastDetails?.isOnline() == true,
            isMe = followManager.isSelf(user.caid),
            twitterUsername = broadcastDetails?.twitterUsername,
            broadcastName = broadcastDetails?.name,
            gameIconImageUrl = broadcastDetails?.game?.iconImageUrl
    )
    val userIcon = when {
        isVerified -> R.drawable.verified
        isCaster -> R.drawable.caster
        else -> 0
    }

    fun isMfaEnabled() = mfaMethod != MfaMethod.NONE

    fun getFollowersString() = compactThousandsNumberFormat(followersCount)
    fun getMyFollowersString() = longFormThousandsNumberFormat(followersCount)
    fun getFollowingString() = compactThousandsNumberFormat(followingCount)
    fun getMyFollowingString() = longFormThousandsNumberFormat(followingCount)
}

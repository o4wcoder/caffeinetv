package tv.caffeine.app.repository

import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    val followManager: FollowManager
) {

    suspend fun getUserProfile(userHandle: String): UserProfile? {
        val userDetails = followManager.userDetails(userHandle) ?: return null
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        return UserProfile(userDetails, broadcastDetails, followManager)
    }
}
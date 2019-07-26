package tv.caffeine.app.repository

import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.session.FollowManager
import java.text.NumberFormat
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    val followManager: FollowManager
) {
    private val numberFormat = NumberFormat.getInstance()

    suspend fun getUserProfile(userHandle: String): UserProfile? {
        val userDetails = followManager.userDetails(userHandle) ?: return null
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        return UserProfile(userDetails, broadcastDetails, numberFormat, followManager)
    }
}
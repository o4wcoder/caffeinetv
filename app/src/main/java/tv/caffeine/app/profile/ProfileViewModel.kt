package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.isOnline
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import java.text.NumberFormat

class ProfileViewModel(
        dispatchConfig: DispatchConfig,
        val followManager: FollowManager
) : CaffeineViewModel(dispatchConfig) {

    private val numberFormat = NumberFormat.getInstance()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = Transformations.map(_userProfile) { it }

    fun load(caid: CAID) = launch {
        val userDetails = followManager.userDetails(caid) ?: return@launch
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        configure(userDetails, broadcastDetails)
    }

    private fun forceLoad(caid: CAID) = launch {
        val userDetails = followManager.loadUserDetails(caid) ?: return@launch
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        configure(userDetails, broadcastDetails)
    }

    private fun configure(userDetails: User, broadcastDetails: Broadcast?) {
        val isLive = broadcastDetails?.isOnline() == true
        val broadcastImageUrl = if (isLive) broadcastDetails?.mainPreviewImageUrl else null
        val userIcon = when {
            userDetails.isVerified -> R.drawable.verified
            userDetails.isCaster -> R.drawable.caster
            else -> 0
        }
        _userProfile.value = UserProfile(
                userDetails.username,
                userDetails.name,
                userDetails.email,
                userDetails.emailVerified,
                numberFormat.format(userDetails.followersCount),
                numberFormat.format(userDetails.followingCount),
                userDetails.bio,
                followManager.isFollowing(userDetails.caid),
                userDetails.isVerified,
                userIcon,
                userDetails.avatarImageUrl,
                userDetails.mfaMethod,
                broadcastImageUrl,
                isLive,
                followManager.isSelf(userDetails.caid),
                twitterUsername = broadcastDetails?.twitterUsername,
                broadcastName = broadcastDetails?.name
        )
    }

    fun follow(caid: CAID): LiveData<CaffeineEmptyResult> {
        val liveData = MutableLiveData<CaffeineEmptyResult>()
        launch {
            val result = followManager.followUser(caid)
            forceLoad(caid)
            liveData.value = result
        }
        return Transformations.map(liveData) { it }
    }

    fun unfollow(caid: CAID) = launch {
        followManager.unfollowUser(caid)
        forceLoad(caid)
    }

}

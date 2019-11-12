package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    val followManager: FollowManager
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile.map { it }

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing.map { it }

    fun load(userHandle: String) = viewModelScope.launch {
        val userDetails = followManager.userDetails(userHandle) ?: return@launch
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        configure(userDetails, broadcastDetails)
    }

    /**
     * Force load when the UI relies on whether the broadcaster is live.
     */
    fun forceLoad(caid: CAID): LiveData<UserProfile> {
        viewModelScope.launch {
            val userDetails = followManager.loadUserDetails(caid) ?: return@launch
            val broadcastDetails = followManager.broadcastDetails(userDetails)
            configure(userDetails, broadcastDetails)
        }
        return userProfile
    }

    private fun configure(userDetails: User, broadcastDetails: Broadcast?) {
        val profile = UserProfile(userDetails, broadcastDetails, followManager)
        _userProfile.value = profile
        // TODO: shouldShowFollow is legacy from classic UI where we hid the follow button when followed
        // TODO: shouldShowFollow == true == NOT FOLLOWING
        // TODO: when classic UI goes away change this name
        val isFollowingUser = !profile.shouldShowFollow
        if (_isFollowing.value == null || _isFollowing.value != isFollowingUser) {
            _isFollowing.value = isFollowingUser
        }
    }

    fun follow(caid: CAID): LiveData<CaffeineEmptyResult> {
        _isFollowing.value = true
        val liveData = MutableLiveData<CaffeineEmptyResult>()
        viewModelScope.launch {
            val result = followManager.followUser(caid)
            forceLoad(caid)
            liveData.value = result
        }
        return liveData.map { it }
    }

    fun unfollow(caid: CAID) = viewModelScope.launch {
        _isFollowing.value = false
        followManager.unfollowUser(caid)
        forceLoad(caid)
    }
}

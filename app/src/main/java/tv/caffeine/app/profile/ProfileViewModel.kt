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
import java.text.NumberFormat
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    val followManager: FollowManager
) : ViewModel() {

    private val numberFormat = NumberFormat.getInstance()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile.map { it }

    fun load(userHandle: String) = viewModelScope.launch {
        val userDetails = followManager.userDetails(userHandle) ?: return@launch
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        configure(userDetails, broadcastDetails)
    }

    /**
     * Force load when the UI relies on whether the broadcaster is live.
     */
    fun forceLoad(caid: CAID) = viewModelScope.launch {
        val userDetails = followManager.loadUserDetails(caid) ?: return@launch
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        configure(userDetails, broadcastDetails)
    }

    private fun configure(userDetails: User, broadcastDetails: Broadcast?) {
        _userProfile.value = UserProfile(userDetails, broadcastDetails, numberFormat, followManager)
    }

    fun follow(caid: CAID): LiveData<CaffeineEmptyResult> {
        val liveData = MutableLiveData<CaffeineEmptyResult>()
        viewModelScope.launch {
            val result = followManager.followUser(caid)
            forceLoad(caid)
            liveData.value = result
        }
        return liveData.map { it }
    }

    fun unfollow(caid: CAID) = viewModelScope.launch {
        followManager.unfollowUser(caid)
        forceLoad(caid)
    }
}

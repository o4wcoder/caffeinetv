package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.Broadcast
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

    private val user = MutableLiveData<User>()
    private val broadcast = MutableLiveData<Broadcast>()

    val username: LiveData<String> = Transformations.map(user) { it.username }
    val name: LiveData<String> = Transformations.map(user) { it.name }
    val followersCount: LiveData<String> = Transformations.map(user) { numberFormat.format(it.followersCount) }
    val followingCount: LiveData<String> = Transformations.map(user) { numberFormat.format(it.followingCount) }
    val bio: LiveData<String> = Transformations.map(user) { it.bio }

    val isFollowed: LiveData<Boolean> = Transformations.map(user) { followManager.isFollowing(it.caid) }
    val isVerified: LiveData<Boolean> = Transformations.map(user) { it.isVerified }

    val avatarImageUrl: LiveData<String> = Transformations.map(user) { it.avatarImageUrl }
    val stageImageUrl: LiveData<String> = Transformations.map(broadcast) { it?.previewImageUrl }

    fun load(caid: String) = launch {
        val userDetails = followManager.userDetails(caid) ?: return@launch
        val broadcastDetails = followManager.broadcastDetails(userDetails)
        user.value = userDetails
        broadcast.value = if (broadcastDetails?.isOnline() == true) broadcastDetails else null
    }

    private fun forceLoad(caid: String) = launch {
        val userDetails = followManager.loadUserDetails(caid) ?: return@launch
        user.value = userDetails
    }

    fun follow(caid: String): LiveData<CaffeineEmptyResult> {
        val liveData = MutableLiveData<CaffeineEmptyResult>()
        launch {
            val result = followManager.followUser(caid)
            forceLoad(caid)
            liveData.value = result
        }
        return Transformations.map(liveData) { it }
    }

    fun unfollow(caid: String) = launch {
        followManager.unfollowUser(caid)
        forceLoad(caid)
    }

}

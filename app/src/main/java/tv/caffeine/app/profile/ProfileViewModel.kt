package tv.caffeine.app.profile

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel

class ProfileViewModel(val followManager: FollowManager) : CaffeineViewModel() {
    val username = MutableLiveData<String>()
    val name = MutableLiveData<String>()
    val followersCount = MutableLiveData<String>()
    val followingCount = MutableLiveData<String>()
    val bio = MutableLiveData<String>()

    val isFollowed = MutableLiveData<Boolean>()
    val isVerified = MutableLiveData<Boolean>()

    val avatarImageUrl = MutableLiveData<String>()
    val stageImageUrl = MutableLiveData<String>()

    fun load(caid: String) {
        launch {
            val userDetails = followManager.userDetails(caid)
            val broadcast = followManager.broadcastDetails(userDetails)
            withContext(Dispatchers.Main) {
                username.value = userDetails.username
                name.value = userDetails.name
                followersCount.value = userDetails.followersCount.toString()
                followingCount.value = userDetails.followingCount.toString()
                bio.value = userDetails.bio
                isFollowed.value = followManager.isFollowing(caid)
                isVerified.value = userDetails.isVerified
                avatarImageUrl.value = userDetails.avatarImageUrl
                if (broadcast.state == Broadcast.State.ONLINE) {
                    stageImageUrl.value = broadcast.previewImageUrl
                }
            }
        }
    }

}
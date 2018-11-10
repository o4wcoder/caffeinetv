package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.isOnline
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel

class ProfileViewModel(val followManager: FollowManager) : CaffeineViewModel() {
    private val user = MutableLiveData<User>()
    private val broadcast = MutableLiveData<Broadcast>()

    val username: LiveData<String> = Transformations.map(user) { it.username }
    val name: LiveData<String> = Transformations.map(user) { it.name }
    val followersCount: LiveData<String> = Transformations.map(user) { it.followersCount.toString() }
    val followingCount: LiveData<String> = Transformations.map(user) { it.followingCount.toString() }
    val bio: LiveData<String> = Transformations.map(user) { it.bio }

    val isFollowed: LiveData<Boolean> = Transformations.map(user) { followManager.isFollowing(it.caid) }
    val isVerified: LiveData<Boolean> = Transformations.map(user) { it.isVerified }

    val avatarImageUrl: LiveData<String> = Transformations.map(user) { it.avatarImageUrl }
    val stageImageUrl: LiveData<String> = Transformations.map(broadcast) { it?.previewImageUrl }

    fun load(caid: String) {
        launch {
            val userDetails = followManager.userDetails(caid) ?: return@launch
            val broadcastDetails = followManager.broadcastDetails(userDetails)
            withContext(Dispatchers.Main) {
                user.value = userDetails
                broadcast.value = if (broadcastDetails?.isOnline() == true) broadcastDetails else null
            }
        }
    }

}

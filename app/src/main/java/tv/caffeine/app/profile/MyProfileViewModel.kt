package tv.caffeine.app.profile

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.*
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel

class MyProfileViewModel(
        private val usersService: UsersService,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val gson: Gson
) : CaffeineViewModel() {
    val username = MutableLiveData<String>()
    val name = MutableLiveData<String>()
    val followersCount = MutableLiveData<String>()
    val followingCount = MutableLiveData<String>()
    val bio = MutableLiveData<String>()

    val isVerified = MutableLiveData<Boolean>()

    val avatarImageUrl = MutableLiveData<String>()

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid?: return
        launch {
            getUserProfile(caid)?.let { updateViewModel(it) }
        }
    }

    private suspend fun getUserProfile(caid: String) = followManager.userDetails(caid)

    private suspend fun updateViewModel(user: User) = withContext(Dispatchers.Main) {
        username.value = user.username
        name.value = user.name
        followersCount.value = user.followersCount.toString()
        followingCount.value = user.followingCount.toString()
        avatarImageUrl.value = user.avatarImageUrl
        isVerified.value = user.isVerified

        // not shown on My Profile
        bio.value = user.bio
    }

    fun updateName(name: String) {
        updateUser(name = name)
    }

    fun updateBio(bio: String) {
        updateUser(bio = bio)
    }

    private fun updateUser(name: String? = null, bio: String? = null) {
        tokenStore.caid?.let { caid ->
            launch {
                val userUpdateBody = UserUpdateBody(UserUpdateDetails(name, bio, null))
                val result = usersService.updateUser(caid, userUpdateBody).awaitAndParseErrors(gson)
                if (result is CaffeineResult.Success) {
                    updateViewModel(result.value.user)
                }
            }
        }
    }
}

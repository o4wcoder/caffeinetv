package tv.caffeine.app.profile

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.UserUpdateBody
import tv.caffeine.app.api.model.UserUpdateDetails
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel

class MyProfileViewModel(
        val accountsService: AccountsService,
        val usersService: UsersService,
        val tokenStore: TokenStore,
        val followManager: FollowManager
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
        tokenStore.caid?.let {
            launch {
                val self = followManager.userDetails(it) ?: return@launch
                withContext(Dispatchers.Main) {
                    username.value = self.username
                    name.value = self.name
                    followersCount.value = self.followersCount.toString()
                    followingCount.value = self.followingCount.toString()
                    avatarImageUrl.value = self.avatarImageUrl
                    isVerified.value = self.isVerified

                    // not shown on My Profile
                    bio.value = self.bio
                }
            }
        }
    }

    fun updateName(name: String) {
        tokenStore.caid?.let { caid ->
            launch {
                usersService.updateUser(caid, UserUpdateBody(UserUpdateDetails(name, null, null))).await()
            }
        }
    }
}

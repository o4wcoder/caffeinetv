package tv.caffeine.app.profile

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
        private val uploadAvatarUseCase: UploadAvatarUseCase,
        private val gson: Gson
) : CaffeineViewModel() {

    private val myProfile = MutableLiveData<User>()

    val username: LiveData<String> = Transformations.map(myProfile) { it.username }
    val name: LiveData<String> = Transformations.map(myProfile) { it.name }
    val followersCount: LiveData<String> = Transformations.map(myProfile) { it.followingCount.toString() }
    val followingCount: LiveData<String> = Transformations.map(myProfile) { it.followingCount.toString() }
    val bio: LiveData<String> = Transformations.map(myProfile) { it.bio }

    val isVerified: LiveData<Boolean> = Transformations.map(myProfile) { it.isVerified }

    val avatarImageUrl: LiveData<String> = Transformations.map(myProfile) { it.avatarImageUrl }

    private var loadJob: Job? = null
        set(value) {
            if (field == value) return
            field?.cancel()
            field = value
        }

    init {
        load()
    }

    fun reload() = load(forceLoad = true)

    private fun load(forceLoad: Boolean = false) {
        val caid = tokenStore.caid?: return
        loadJob = launch {
            val userProfile = if (forceLoad) loadUserProfile(caid) else getUserProfile(caid)
            userProfile?.let { updateViewModel(it) }
        }
    }

    private suspend fun getUserProfile(caid: String) = followManager.userDetails(caid)

    private suspend fun loadUserProfile(caid: String) = followManager.loadUserDetails(caid)

    private suspend fun updateViewModel(user: User) = withContext(Dispatchers.Main) {
        myProfile.value = user
    }

    fun updateName(name: String) {
        updateUser(name = name)
    }

    fun updateBio(bio: String) {
        updateUser(bio = bio)
    }

    fun uploadAvatar(bitmap: Bitmap) {
        launch {
            val result = uploadAvatarUseCase(bitmap)
            if (result is CaffeineResult.Success) {
                reload()
            }
        }
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

package tv.caffeine.app.profile

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.MfaMethod
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import java.text.NumberFormat

class MyProfileViewModel(
        dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val uploadAvatarUseCase: UploadAvatarUseCase
) : CaffeineViewModel(dispatchConfig) {

    private val numberFormat = NumberFormat.getInstance()

    private val myProfile = MutableLiveData<User>()

    val username: LiveData<String> = Transformations.map(myProfile) { it.username }
    val email: LiveData<String> = Transformations.map(myProfile) { it.email }
    val name: LiveData<String> = Transformations.map(myProfile) { it.name }
    val followersCount: LiveData<String> = Transformations.map(myProfile) { numberFormat.format(it.followersCount) }
    val followingCount: LiveData<String> = Transformations.map(myProfile) { numberFormat.format(it.followingCount) }
    val bio: LiveData<String> = Transformations.map(myProfile) { it.bio }
    val mfaMethod: LiveData<MfaMethod> = Transformations.map(myProfile) { it.mfaMethod }

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

    private fun load() {
        val caid = tokenStore.caid?: return
        loadJob = launch {
            getUserProfile(caid)?.let { updateViewModel(it) }
            loadUserProfile(caid)?.let { updateViewModel(it) }
        }
    }

    private suspend fun getUserProfile(caid: String) = followManager.userDetails(caid)

    private suspend fun loadUserProfile(caid: String) = followManager.loadUserDetails(caid)

    private suspend fun updateViewModel(user: User) = withContext(dispatchConfig.main) {
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
                load()
            }
        }
    }

    private fun updateUser(name: String? = null, bio: String? = null) {
        val caid = tokenStore.caid?: return
        launch {
            val result = followManager.updateUser(caid, name, bio)
            when (result) {
                is CaffeineResult.Success -> updateViewModel(result.value.user)
            }
        }
    }
}

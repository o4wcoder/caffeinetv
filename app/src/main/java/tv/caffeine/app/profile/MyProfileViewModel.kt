package tv.caffeine.app.profile

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
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

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = Transformations.map(_userProfile) { it }

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

    private suspend fun getUserProfile(caid: CAID) = followManager.userDetails(caid)

    private suspend fun loadUserProfile(caid: CAID) = followManager.loadUserDetails(caid)

    private suspend fun updateViewModel(user: User) = withContext(dispatchConfig.main) {
        val userIcon = when {
            user.isVerified -> R.drawable.verified
            user.isCaster -> R.drawable.caster
            else -> 0
        }
        _userProfile.value = UserProfile(
                user.username,
                user.name,
                user.email,
                user.emailVerified,
                numberFormat.format(user.followersCount),
                numberFormat.format(user.followingCount),
                user.bio,
                false,
                user.isVerified,
                userIcon,
                user.avatarImageUrl,
                user.mfaMethod,
                null,
                false
        )
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

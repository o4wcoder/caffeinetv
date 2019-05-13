package tv.caffeine.app.profile

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import java.text.NumberFormat
import javax.inject.Inject

class MyProfileViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val followManager: FollowManager,
    private val uploadAvatarUseCase: UploadAvatarUseCase
) : ViewModel() {

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
        val caid = tokenStore.caid ?: return
        loadJob = viewModelScope.launch {
            getUserProfile(caid)?.let { updateViewModel(it) }
            loadUserProfile(caid)?.let { updateViewModel(it) }
        }
    }

    private suspend fun getUserProfile(caid: CAID) = followManager.userDetails(caid)

    private suspend fun loadUserProfile(caid: CAID) = followManager.loadUserDetails(caid)

    private suspend fun updateViewModel(user: User) = withContext(Dispatchers.Main) {
        _userProfile.value = UserProfile(user, null, numberFormat, followManager)
    }

    fun updateName(name: String) {
        updateUser(name = name)
    }

    fun updateBio(bio: String) {
        updateUser(bio = bio)
    }

    fun uploadAvatar(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = uploadAvatarUseCase(bitmap)
            if (result is CaffeineResult.Success) {
                load()
            }
        }
    }

    private fun updateUser(name: String? = null, bio: String? = null) {
        val caid = tokenStore.caid ?: return
        viewModelScope.launch {
            val result = followManager.updateUser(caid, name, bio)
            when (result) {
                is CaffeineResult.Success -> updateViewModel(result.value.user)
            }
        }
    }
}

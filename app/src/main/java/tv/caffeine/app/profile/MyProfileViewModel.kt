package tv.caffeine.app.profile

import android.graphics.Bitmap
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.facebook.login.LoginManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.SecureSettingsStorage
import java.text.NumberFormat
import javax.inject.Inject

class MyProfileViewModel @Inject constructor(
    private val accountsService: AccountsService,
    private val tokenStore: TokenStore,
    private val authWatcher: AuthWatcher,
    private val followManager: FollowManager,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val facebookLoginManager: LoginManager,
    private val secureSettingsStorage: SecureSettingsStorage,
    private val gson: Gson
) : ViewModel() {

    private val numberFormat = NumberFormat.getInstance()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile.map { it }

    private val _signOutComplete = MutableLiveData<Event<Boolean>>()
    val signOutComplete: LiveData<Event<Boolean>> = _signOutComplete.map { it }

    /** Observable state for sign out progress indicator.  */
    var signOutLoading = ObservableBoolean(false)
        private set

    private var loadJob: Job? = null
        set(value) {
            if (field == value) return
            field?.cancel()
            field = value
        }

    init {
        load()
    }

    fun load() {
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

    fun signOut() {
        signOutLoading.set(true)
        viewModelScope.launch {
            val authWatcherResult = authWatcher.onSignOut(secureSettingsStorage.deviceId)
            when (authWatcherResult) {
                is CaffeineEmptyResult.Success -> secureSettingsStorage.deviceId = null
                is CaffeineEmptyResult.Error -> Timber.e("Error deleting device on server ${authWatcherResult.error}")
                is CaffeineEmptyResult.Failure -> Timber.e(authWatcherResult.throwable)
            }
            tokenStore.clear()
            facebookLoginManager.logOut()
            val accountsServiceResult = accountsService.signOut().awaitEmptyAndParseErrors(gson)
            when (accountsServiceResult) {
                is CaffeineEmptyResult.Success -> Timber.d("Signed out successfully")
                is CaffeineEmptyResult.Error -> Timber.e("Failed to sign out ${accountsServiceResult.error}")
                is CaffeineEmptyResult.Failure -> Timber.e(accountsServiceResult.throwable)
            }
            signOutLoading.set(false)
            _signOutComplete.postValue(Event(true))
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

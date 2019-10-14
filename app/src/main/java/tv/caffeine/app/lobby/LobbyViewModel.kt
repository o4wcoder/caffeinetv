package tv.caffeine.app.lobby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.VersionCheckError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.feature.LoadFeatureConfigUseCase
import tv.caffeine.app.lobby.type.Page
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.update.IsVersionSupportedCheckUseCase
import javax.inject.Inject

class LobbyViewModel @Inject constructor(
    private val followManager: FollowManager,
    private val loadLobbyUseCase: LoadLobbyUseCase,
    private val loadFeatureConfigUseCase: LoadFeatureConfigUseCase,
    private val isVersionSupportedCheckUseCase: IsVersionSupportedCheckUseCase,
    private val accountRepository: AccountRepository,
    private val lobbyRepository: LobbyRepository,
    private val releaseDesignConfig: ReleaseDesignConfig
) : ViewModel() {
    private val _lobby = MutableLiveData<CaffeineResult<Lobby>>()
    private val _lobbyV5 = MutableLiveData<CaffeineResult<LobbyQuery.Data>>()
    private val _lobbyDetail = MutableLiveData<CaffeineResult<DetailPageQuery.Data>>()
    private val _emailVerificationUser = MutableLiveData<User>()

    val lobby: LiveData<CaffeineResult<Lobby>> = _lobby.map { it }
    val lobbyV5: LiveData<CaffeineResult<LobbyQuery.Data>> = _lobbyV5.map { it }
    val lobbyDetail: LiveData<CaffeineResult<DetailPageQuery.Data>> = _lobbyDetail.map { it }
    val emailVerificationUser = _emailVerificationUser.map { it }

    private var refreshJob: Job? = null
    private var isFirstLoad = true

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val isVersionSupported = isVersionSupportedCheckUseCase()
            if (isVersionSupported is CaffeineEmptyResult.Error) {
                _lobby.value = CaffeineResult.Error(VersionCheckError())
                return@launch
            }
            followManager.refreshFollowedUsers()
            loadLobby()
            if (releaseDesignConfig.isReleaseDesignActive()) {
                /*
                In the classic UI, the user info for email verification is loaded in LobbySwipeFragment.
                It is fine because we do not need the email info. We just need the isVerified field.
                And the UI is above the ViewPager so it makes sense to leave it there. In the release UI,
                We need the email info for the "resend email" button. Without the proper credentials we
                can't get the email info from the API. That's why it can't be the first API call in the
                auth part.
                */
                loadEmailVerificationUser()
            }
        }
    }

    fun refreshV5(page: Page, onRefresh: () -> Unit) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val isVersionSupported = isVersionSupportedCheckUseCase()
            if (isVersionSupported is CaffeineEmptyResult.Error) {
                _lobbyV5.value = CaffeineResult.Error(VersionCheckError())
                return@launch
            }
            followManager.refreshFollowedUsers()
            loadLobbyV5(page)
            loadEmailVerificationUser()
            onRefresh()
        }
    }

    fun refreshDetail(cardId: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            followManager.refreshFollowedUsers()
            loadLobbyDetail(cardId)
        }
    }

    private suspend fun loadLobby() = coroutineScope {
        _lobby.value = loadLobbyUseCase()
        if (isFirstLoad) {
            isFirstLoad = false
            // load the feature config after the lobby is loaded for better cold start perf
            loadFeatureConfigUseCase()
        }
    }

    private suspend fun loadLobbyV5(page: Page) = coroutineScope {
        _lobbyV5.value = lobbyRepository.loadLobbyV5(page)
        if (isFirstLoad) {
            isFirstLoad = false
            // load the feature config after the lobby is loaded for better cold start perf
            loadFeatureConfigUseCase()
        }
    }

    private suspend fun loadLobbyDetail(cardId: String) = coroutineScope {
        _lobbyDetail.value = lobbyRepository.loadLobbyDetail(cardId)
    }

    private suspend fun loadEmailVerificationUser() = coroutineScope {
        followManager.loadMyUserDetails()?.let { user ->
            _emailVerificationUser.value = user
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            val result = accountRepository.resendVerification()
            when (result) {
                is CaffeineEmptyResult.Success -> Timber.d("Successfully resent verification email")
                is CaffeineEmptyResult.Error -> Timber.e("Error resending verification email")
                is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
            }
        }
    }
}

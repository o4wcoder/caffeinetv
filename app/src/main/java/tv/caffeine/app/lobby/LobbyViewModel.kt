package tv.caffeine.app.lobby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.VersionCheckError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.LoadFeatureConfigUseCase
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.update.IsVersionSupportedCheckUseCase
import javax.inject.Inject

class LobbyViewModel @Inject constructor(
    private val followManager: FollowManager,
    private val loadLobbyUseCase: LoadLobbyUseCase,
    private val loadFeatureConfigUseCase: LoadFeatureConfigUseCase,
    private val isVersionSupportedCheckUseCase: IsVersionSupportedCheckUseCase
) : ViewModel() {
    private val _lobby = MutableLiveData<CaffeineResult<Lobby>>()

    val lobby: LiveData<CaffeineResult<Lobby>> = _lobby.map { it }

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
        }
    }

    private suspend fun loadLobby() = coroutineScope {
        _lobby.value = loadLobbyUseCase()
        if (isFirstLoad) {
            isFirstLoad = false
            // load the feature config after the lobby is loaded for better cold start perf
            loadFeatureConfigUseCase(Feature.BROADCAST)
        }
    }
}

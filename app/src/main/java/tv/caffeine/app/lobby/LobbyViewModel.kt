package tv.caffeine.app.lobby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.ui.CaffeineViewModel
import java.util.concurrent.TimeUnit

class LobbyViewModel(private val loadLobbyUseCase: LoadLobbyUseCase) : CaffeineViewModel() {
    val lobby: LiveData<CaffeineResult<Lobby>> get() = _lobby

    private val _lobby = MutableLiveData<CaffeineResult<Lobby>>()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = launch {
            while(isActive) {
                loadLobby()
                delay(TimeUnit.SECONDS.toMillis(30))
            }
        }
    }

    private suspend fun loadLobby() = coroutineScope {
        _lobby.value = runCatching { loadLobbyUseCase() }.fold({ it }, { CaffeineResult.Failure(it) })
    }

}

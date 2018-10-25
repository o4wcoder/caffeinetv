package tv.caffeine.app.lobby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class LobbyViewModel(private val loadLobbyUseCase: LoadLobbyUseCase) : ViewModel(), CoroutineScope {
    val lobby: LiveData<CaffeineResult<Lobby>> get() = _lobby

    private val _lobby = MutableLiveData<CaffeineResult<Lobby>>()

    private var job = Job()
    private var refreshJob: Job? = null

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = launch(context = coroutineContext) {
            while(isActive) {
                loadLobby()
                delay(TimeUnit.SECONDS.toMillis(30))
            }
        }
    }

    private suspend fun loadLobby() = coroutineScope {
        _lobby.value = runCatching { loadLobbyUseCase() }.fold({ CaffeineResult.Success(it) }, { CaffeineResult.Failure(it) })
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

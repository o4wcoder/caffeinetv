package tv.caffeine.app.lobby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import tv.caffeine.app.api.model.Lobby
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class LobbyViewModel(private val loadLobbyUseCase: LoadLobbyUseCase) : ViewModel(), CoroutineScope {
    val lobby: LiveData<Lobby> get() = _lobby
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _lobby = MutableLiveData<Lobby>()
    private val _isLoading = MutableLiveData<Boolean>().apply {
        value = false
    }

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
        _isLoading.value = true
        val result = loadLobbyUseCase()
        _lobby.value = result
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

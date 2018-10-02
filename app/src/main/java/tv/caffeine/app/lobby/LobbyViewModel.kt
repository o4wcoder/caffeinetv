package tv.caffeine.app.lobby

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.model.Lobby
import java.util.concurrent.TimeUnit

class LobbyViewModel(private val lobbyService: LobbyService) : ViewModel() {
    val lobby: MutableLiveData<Lobby.Result> = MutableLiveData()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.Default) {
            while(isActive) {
                loadLobby()
                delay(30, TimeUnit.SECONDS)
            }
        }
    }

    private fun loadLobby() {
        lobbyService.newLobby().enqueue(object: Callback<Lobby.Result?> {
            override fun onFailure(call: Call<Lobby.Result?>?, t: Throwable?) {
                Timber.e(t, "NEWLOBBY Failed to get the new lobby")
            }

            override fun onResponse(call: Call<Lobby.Result?>?, response: Response<Lobby.Result?>?) {
                Timber.d("NEWLOBBY Got the new lobby ${response?.body()}")
                response?.body()?.let { lobby.value = it }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}

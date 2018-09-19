package tv.caffeine.app.lobby

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.Api
import tv.caffeine.app.api.LobbyService
import java.util.concurrent.TimeUnit

class LobbyViewModel(private val lobbyService: LobbyService) : ViewModel() {
    val lobby: MutableLiveData<Api.v3.Lobby.Result> = MutableLiveData()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = launch {
            while(isActive) {
                loadLobby()
                delay(30, TimeUnit.SECONDS)
            }
        }
    }

    private fun loadLobby() {
        lobbyService.newLobby().enqueue(object: Callback<Api.v3.Lobby.Result?> {
            override fun onFailure(call: Call<Api.v3.Lobby.Result?>?, t: Throwable?) {
                Timber.e(t, "NEWLOBBY Failed to get the new lobby")
            }

            override fun onResponse(call: Call<Api.v3.Lobby.Result?>?, response: Response<Api.v3.Lobby.Result?>?) {
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

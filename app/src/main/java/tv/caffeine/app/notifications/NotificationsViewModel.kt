package tv.caffeine.app.notifications

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import tv.caffeine.app.api.FollowRecord
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.TokenStore

class NotificationsViewModel(private val usersService: UsersService, private val tokenStore: TokenStore) : ViewModel() {
    val followers: MutableLiveData<List<FollowRecord>> = MutableLiveData()
    private var job: Job? = null

    fun refresh() {
        val caid = tokenStore.caid ?: return
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.Default) {
            val result = usersService.listFollowers(caid).await()
            launch(Dispatchers.Main) {
                followers.value = result
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}

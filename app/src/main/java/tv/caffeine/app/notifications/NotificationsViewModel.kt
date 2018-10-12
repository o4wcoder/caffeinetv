package tv.caffeine.app.notifications

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ui.CaffeineViewModel

class NotificationsViewModel(private val usersService: UsersService, private val tokenStore: TokenStore) : CaffeineViewModel() {
    val followers: MutableLiveData<List<CaidRecord.FollowRecord>> = MutableLiveData()

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        launch {
            val result = usersService.listFollowers(caid).await()
            withContext(Dispatchers.Main) {
                followers.value = result
            }
        }
    }
}

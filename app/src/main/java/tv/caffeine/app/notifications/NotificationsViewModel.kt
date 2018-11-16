package tv.caffeine.app.notifications

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

class NotificationsViewModel(
        dispatchConfig: DispatchConfig,
        private val usersService: UsersService,
        private val tokenStore: TokenStore
) : CaffeineViewModel(dispatchConfig) {
    val followers: MutableLiveData<List<CaidRecord.FollowRecord>> = MutableLiveData()

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        launch {
            val result = usersService.listFollowers(caid).await()
            followers.value = result
        }
    }
}

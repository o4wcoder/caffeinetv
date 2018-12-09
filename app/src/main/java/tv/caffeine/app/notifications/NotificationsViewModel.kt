package tv.caffeine.app.notifications

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

class NotificationsViewModel(
        dispatchConfig: DispatchConfig,
        private val gson: Gson,
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
            val result = usersService.listFollowers(caid).awaitAndParseErrors(gson)
            when(result) {
                is CaffeineResult.Success -> followers.value = result.value
                is CaffeineResult.Error -> Timber.e(Exception("Error loading followers ${result.error}"))
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }
}

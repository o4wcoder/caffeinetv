package tv.caffeine.app.notifications

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class NotificationsViewModel @Inject constructor(
    dispatchConfig: DispatchConfig,
    private val gson: Gson,
    private val usersService: UsersService,
    private val followManager: FollowManager,
    private val tokenStore: TokenStore
) : CaffeineViewModel(dispatchConfig) {
    val notifications: MutableLiveData<List<CaffeineNotification>> = MutableLiveData()

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        launch {
            val currentUser = followManager.loadUserDetails(caid) ?: return@launch
            val referenceTimestamp = currentUser.notificationsLastViewedAt
            val result = usersService.listFollowers(caid).awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> notifications.value = result.value.map { FollowNotification(it, isNewer(it.followedAt, referenceTimestamp)) }
                is CaffeineResult.Error -> Timber.e("Error loading followers ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    private fun isNewer(timestamp: ZonedDateTime?, referenceTimestamp: ZonedDateTime?): Boolean {
        if (timestamp == null || referenceTimestamp == null) return true
        return timestamp.isAfter(referenceTimestamp)
    }

    fun markNotificationsViewed() = launch {
        val caid = tokenStore.caid ?: return@launch
        val result = usersService.notificationsViewed(caid).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> Timber.d("Successfully marked notifications viewed")
            is CaffeineResult.Error -> Timber.d("Error marking notifications viewed ${result.error}")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }
}

sealed class CaffeineNotification
data class FollowNotification(val caid: CaidRecord, val isNew: Boolean) : CaffeineNotification()

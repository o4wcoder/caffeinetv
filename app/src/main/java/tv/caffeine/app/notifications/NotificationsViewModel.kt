package tv.caffeine.app.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import tv.caffeine.app.api.TransactionHistoryItem
import tv.caffeine.app.api.convert
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ext.isNewer
import tv.caffeine.app.repository.TransactionHistoryRepository
import tv.caffeine.app.repository.UsersRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.util.toZonedDateTime
import javax.inject.Inject

class NotificationsViewModel @Inject constructor(
    private val gson: Gson,
    private val usersRepository: UsersRepository,
    private val transactionHistoryRepository: TransactionHistoryRepository,
    private val followManager: FollowManager,
    private val tokenStore: TokenStore,
    private val releaseDesignConfig: ReleaseDesignConfig
) : ViewModel() {
    val _notifications: MutableLiveData<List<CaffeineNotification>> = MutableLiveData()
    val notifications: LiveData<List<CaffeineNotification>> = _notifications.map { it }

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        viewModelScope.launch {
            val currentUser = followManager.loadUserDetails(caid) ?: return@launch
            val referenceTimestamp = currentUser.notificationsLastViewedAt
            var allNotifications: MutableList<CaffeineNotification>? = null

            val followersResult = usersRepository.getFollowersList(caid)
            when (followersResult) {
                is CaffeineResult.Success -> allNotifications = followersResult.value.followers.map {
                    FollowNotification(it, it.followedAt.isNewer(referenceTimestamp)) }.toMutableList()
                is CaffeineResult.Error -> Timber.e("Error loading followers ${followersResult.error}")
                is CaffeineResult.Failure -> Timber.e(followersResult.throwable)
            }

            if (releaseDesignConfig.isReleaseDesignActive()) {
                val receivedDigitalItemsResult = transactionHistoryRepository.getTransactionHistory()
                when (receivedDigitalItemsResult) {
                    is CaffeineResult.Success -> {
                        val receivedDigitalsItems = receivedDigitalItemsResult.value.payload.transactions.state.map {
                            it.convert() }.filterIsInstance<TransactionHistoryItem.ReceiveDigitalItem>()
                        allNotifications?.addAll(receivedDigitalsItems.map {
                            ReceivedDigitalItemNotification(it, it.createdAt.toZonedDateTime().isNewer(referenceTimestamp)) })
                    }
                    is CaffeineResult.Error -> Timber.e("Error loading received digital items for count ${receivedDigitalItemsResult.error}")
                    is CaffeineResult.Failure -> Timber.e("${receivedDigitalItemsResult.throwable}")
                }
            }

            allNotifications?.sortByDescending { it.dateToCompare }

            _notifications.value = allNotifications
        }
    }

    fun markNotificationsViewed() = viewModelScope.launch {
        val caid = tokenStore.caid ?: return@launch
        val result = usersRepository.markNotificationsViewed(caid)
        when (result) {
            is CaffeineResult.Success -> Timber.d("Successfully marked notifications viewed")
            is CaffeineResult.Error -> Timber.d("Error marking notifications viewed ${result.error}")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }
}

sealed class CaffeineNotification {
    val dateToCompare: ZonedDateTime?
        get() = when (this) {
            is FollowNotification -> {
                if (this.caid is CaidRecord.FollowRecord && this.caid.followedAt !== null) this.caid.followedAt else null
            }
            is ReceivedDigitalItemNotification -> {
                this.digitalItem.createdAt.toZonedDateTime()
            }
        }
}
data class FollowNotification(val caid: CaidRecord, val isNew: Boolean) : CaffeineNotification()
data class ReceivedDigitalItemNotification(val digitalItem: TransactionHistoryItem.ReceiveDigitalItem, val isNew: Boolean) : CaffeineNotification()

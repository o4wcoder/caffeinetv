package tv.caffeine.app.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import tv.caffeine.app.api.TransactionHistoryItem
import tv.caffeine.app.api.convert
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.repository.TransactionHistoryRepository
import tv.caffeine.app.repository.UsersRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
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
                    FollowNotification(it, isNewer(it.followedAt, referenceTimestamp)) }.toMutableList()
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
                            ReceivedDigitalItemNotification(it, isNewer(zonedDateTime(it.createdAt), referenceTimestamp)) }) // TODO: is this how to determine isNew?
                    }
                    is CaffeineResult.Error -> Timber.e("Error loading followers ${receivedDigitalItemsResult.error}")
                    is CaffeineResult.Failure -> Timber.e("${receivedDigitalItemsResult.throwable}")
                }
            }

            _notifications.value = allNotifications
        }
    }

    private fun zonedDateTime(from: Int): ZonedDateTime {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(from.toLong()), ZoneId.systemDefault())
    }

    private fun isNewer(timestamp: ZonedDateTime?, referenceTimestamp: ZonedDateTime?): Boolean {
        if (timestamp == null || referenceTimestamp == null) return true
        return timestamp.isAfter(referenceTimestamp)
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

sealed class CaffeineNotification
data class FollowNotification(val caid: CaidRecord, val isNew: Boolean) : CaffeineNotification()
data class ReceivedDigitalItemNotification(val digitalItem: TransactionHistoryItem.ReceiveDigitalItem, val isNew: Boolean) : CaffeineNotification()

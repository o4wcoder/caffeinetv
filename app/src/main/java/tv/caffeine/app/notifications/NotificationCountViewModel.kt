package tv.caffeine.app.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.TransactionHistoryItem
import tv.caffeine.app.api.convert
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ext.isNewer
import tv.caffeine.app.repository.TransactionHistoryRepository
import tv.caffeine.app.repository.UsersRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.util.toZonedDateTime
import javax.inject.Inject

class NotificationCountViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    private val transactionHistoryRepository: TransactionHistoryRepository,
    private val followManager: FollowManager,
    private val tokenStore: TokenStore,
    private val releaseDesignConfig: ReleaseDesignConfig
) : ViewModel() {
    private val _hasNewNotifications = MutableLiveData<Event<Boolean>>()
    val hasNewNotifications: LiveData<Event<Boolean>> = _hasNewNotifications.map { it }

    init {
        checkNewNotifications()
    }

    private fun checkNewNotifications() {
        val caid = tokenStore.caid ?: return
        viewModelScope.launch {
            val currentUser = followManager.loadUserDetails(caid) ?: return@launch
            val referenceTimestamp = currentUser.notificationsLastViewedAt
            var hasNewNotifications = false

            val followersResult = usersRepository.getFollowersList(caid)
            when (followersResult) {
                is CaffeineResult.Success -> {
                    hasNewNotifications = followersResult.value.followers.count {
                        it.followedAt.isNewer(referenceTimestamp)
                    } > 0
                }
                is CaffeineResult.Error -> Timber.e("Error loading followers for count ${followersResult.error}")
                is CaffeineResult.Failure -> Timber.e(followersResult.throwable)
            }

            if (!hasNewNotifications && releaseDesignConfig.isReleaseDesignActive()) {
                val receivedDigitalItemsResult = transactionHistoryRepository.getTransactionHistory()
                when (receivedDigitalItemsResult) {
                    is CaffeineResult.Success -> {
                        val receivedItems = receivedDigitalItemsResult.value.payload.transactions.state.map { it.convert() }
                        val digitalItems = receivedItems.filterIsInstance<TransactionHistoryItem.ReceiveDigitalItem>()
                        hasNewNotifications = digitalItems.count { it.createdAt.toZonedDateTime().isNewer(referenceTimestamp) } > 0
                    }
                    is CaffeineResult.Error -> Timber.e("Error loading received digital items for count ${receivedDigitalItemsResult.error}")
                    is CaffeineResult.Failure -> Timber.e("${receivedDigitalItemsResult.throwable}")
                }
            }

            _hasNewNotifications.value = Event(hasNewNotifications)
        }
    }
}
package tv.caffeine.app.lobby.notification

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import com.jakewharton.threetenabp.AndroidThreeTen
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.threeten.bp.ZonedDateTime
import tv.caffeine.app.api.DigitalItemAssets
import tv.caffeine.app.api.HugeTransactionHistoryItem
import tv.caffeine.app.api.PaymentsCollection
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.TransactionHistoryItem
import tv.caffeine.app.api.TransactionHistoryPayload
import tv.caffeine.app.api.convert
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.PaginatedFollowers
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.notifications.FollowNotification
import tv.caffeine.app.notifications.NotificationsViewModel
import tv.caffeine.app.notifications.ReceivedDigitalItemNotification
import tv.caffeine.app.repository.TransactionHistoryRepository
import tv.caffeine.app.repository.UsersRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.test.observeForTesting
import tv.caffeine.app.util.CoroutinesTestRule
import tv.caffeine.app.util.toZonedDateTime

@RunWith(RobolectricTestRunner::class)
class NotificationsViewModelTests {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    @MockK private lateinit var fakeGson: Gson
    @MockK private lateinit var fakeUsersRepository: UsersRepository
    @MockK private lateinit var fakeTransactionHistoryRepository: TransactionHistoryRepository
    @MockK private lateinit var fakeFollowManager: FollowManager
    @MockK private lateinit var fakeTokenStore: TokenStore
    @MockK private lateinit var fakeReleaseDesignConfig: ReleaseDesignConfig

    companion object {
        private const val FOLLOWING_COUNT = 42
        private const val FOLLOWERS_COUNT = 1000
        private const val AGE = 99
    }

    private val notificationLastViewed = ZonedDateTime.now()
    private val justUser = User("caid", "username", "name", "email", "avatarImagePath", FOLLOWING_COUNT, FOLLOWERS_COUNT,
        false, false, "broadcastId", "stageId", mapOf(), mapOf(), AGE, "bio", "countryCode", "countryName", "gender", false,
        false, notificationLastViewed, null, false, false)
    private val followRecordsNew = listOf(CaidRecord.FollowRecord("123", notificationLastViewed.plusHours(1L)))
    private val followRecordsOld = listOf(CaidRecord.FollowRecord("123", notificationLastViewed.minusHours(1L)))
    private val paginatedFollowersNew = PaginatedFollowers(0, 100, followRecordsNew)
    private val paginatedFollowersOld = PaginatedFollowers(0, 100, followRecordsOld)

    private val digItemCreatedAtNew = 2080085367
    private val digItemCreatedAtOld = 1565320568
    private val receivedDigitalItemNew = HugeTransactionHistoryItem("", "ReceiveDigitalItem", digItemCreatedAtNew, 1, 1,
        null, null, 1, null, "", "", "", DigitalItemAssets("", "", ""), null, null)
    private val receivedDigitalItemOld = HugeTransactionHistoryItem("", "ReceiveDigitalItem", digItemCreatedAtOld, 1, 1,
        null, null, 1, null, "", "", "", DigitalItemAssets("", "", ""), null, null)
    private val transHistoryPayloadNew = TransactionHistoryPayload(PaymentsCollection(listOf(receivedDigitalItemNew)))
    private val transHistoryPayloadOld = TransactionHistoryPayload(PaymentsCollection(listOf(receivedDigitalItemOld)))

    private lateinit var subject: NotificationsViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { fakeFollowManager.loadUserDetails(any()) } returns justUser
        coEvery { fakeTokenStore.caid } returns "123"
        coEvery { fakeReleaseDesignConfig.isReleaseDesignActive() } returns true
    }

    @Test
    fun `new follow and received digital item records result in new follow notifications`() {
        val result = paginatedFollowersNew
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayloadNew))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationsViewModel(fakeGson, fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.notifications.observeForTesting { notifications ->
            val followNotifications: List<FollowNotification> = notifications.filterIsInstance<FollowNotification>()
            val receiveDigitalItemNotifications: List<ReceivedDigitalItemNotification> = notifications.filterIsInstance<ReceivedDigitalItemNotification>()
            assertTrue(followNotifications[0].isNew)
            assertTrue(receiveDigitalItemNotifications[0].isNew)
        }
    }

    @Test
    fun `old follow and received digital item records result in old follow notifications`() {
        val result = paginatedFollowersOld
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayloadOld))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationsViewModel(fakeGson, fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.notifications.observeForTesting { notifications ->
            val followNotifications: List<FollowNotification> = notifications.filterIsInstance<FollowNotification>()
            val receiveDigitalItemNotifications: List<ReceivedDigitalItemNotification> = notifications.filterIsInstance<ReceivedDigitalItemNotification>()
            assertFalse(followNotifications[0].isNew)
            assertFalse(receiveDigitalItemNotifications[0].isNew)
        }
    }

    @Test
    fun `notifications are sorted from newest to oldest`() {
        val transHistoryPayloadToSort = getReceivedItemsForSortTest()
        val followersToSort = PaginatedFollowers(0, 100, getFollowersForSortTest())

        val result = followersToSort
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayloadToSort))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationsViewModel(fakeGson, fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.notifications.observeForTesting { notifications ->
            val first = notifications[0]
            val fifth = notifications[4]
            val last = notifications[9]
            assertEquals((first as FollowNotification).caid.caid, "1")
            assertEquals((fifth as FollowNotification).caid.caid, "5")
            assertEquals((last as ReceivedDigitalItemNotification).digitalItem.id, "10")
        }
    }

    @Test
    fun `users service list followers is called on load`() {
        val result = paginatedFollowersOld
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayloadOld))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationsViewModel(fakeGson, fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)
        coVerify(exactly = 1) { fakeUsersRepository.getFollowersList(any()) }
    }

    @Test
    fun `transaction repository list is called on load`() {
        val result = paginatedFollowersOld
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayloadOld))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationsViewModel(fakeGson, fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)
        coVerify(exactly = 1) { fakeTransactionHistoryRepository.getTransactionHistory() }
    }

    private fun getFollowersForSortTest(): List<CaidRecord.FollowRecord> {
        return (1..9)
            .filter { it % 2 == 1 }
            .map { CaidRecord.FollowRecord("$it", ZonedDateTime.now().minusMinutes((it * 10).toLong())) }
    }

    private fun getReceivedItemsForSortTest(): TransactionHistoryPayload {
        val list = (2..10)
            .filter { it % 2 == 0 }
            .map { HugeTransactionHistoryItem("$it", "ReceiveDigitalItem", ZonedDateTime.now().minusMinutes(it * 11L).toEpochSecond().toInt(), 1, 1, null, null, 1, null, "", "", "", DigitalItemAssets("", "", ""), null, null) }
        return TransactionHistoryPayload(PaymentsCollection(list))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApp::class)
class CaffeineNotificationTests {

    @Test
    fun `ensure follow notification date to compare is correct`() {
        val testTime = ZonedDateTime.now()
        val followRecord = CaidRecord.FollowRecord("123", testTime)
        val notification = FollowNotification(followRecord, true)
        assertEquals(testTime, notification.dateToCompare)
    }

    @Test
    fun `ensure follow notification with no follow date returns null date to compare`() {
        val followRecord = CaidRecord.FollowRecord("123", null)
        val notification = FollowNotification(followRecord, true)
        assertEquals(null, notification.dateToCompare)
    }

    @Test
    fun `ensure received item notificaion date to compare is correct`() {
        val testTime = 2080085367.toZonedDateTime()
        val receivedItem: TransactionHistoryItem.ReceiveDigitalItem = HugeTransactionHistoryItem("123", "ReceiveDigitalItem", testTime.toEpochSecond().toInt(), 1, 1, null, null, 1, null, "", "", "", DigitalItemAssets("", "", ""), null, null).convert() as TransactionHistoryItem.ReceiveDigitalItem
        val notification = ReceivedDigitalItemNotification(receivedItem, true)
        assertEquals(testTime, notification.dateToCompare)
    }
}

class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}

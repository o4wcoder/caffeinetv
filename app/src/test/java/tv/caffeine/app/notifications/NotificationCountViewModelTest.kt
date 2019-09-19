package tv.caffeine.app.notifications

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.ZonedDateTime
import tv.caffeine.app.api.DigitalItemAssets
import tv.caffeine.app.api.HugeTransactionHistoryItem
import tv.caffeine.app.api.PaymentsCollection
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.TransactionHistoryPayload
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.PaginatedFollowers
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.repository.TransactionHistoryRepository
import tv.caffeine.app.repository.UsersRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.test.observeForTesting
import tv.caffeine.app.util.CoroutinesTestRule

@RunWith(RobolectricTestRunner::class)
class NotificationCountViewModelTest {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    @MockK private lateinit var fakeUsersRepository: UsersRepository
    @MockK private lateinit var fakeTransactionHistoryRepository: TransactionHistoryRepository
    @MockK private lateinit var fakeFollowManager: FollowManager
    @MockK private lateinit var fakeTokenStore: TokenStore
    @MockK private lateinit var fakeReleaseDesignConfig: ReleaseDesignConfig

    private val notificationLastViewed = ZonedDateTime.now()
    private val justUser = User("caid", "username", "name", "email", "avatarImagePath", 25, 25, false, false, "broadcastId",
        "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, notificationLastViewed,
        null, false, false)
    lateinit var subject: NotificationCountViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { fakeFollowManager.loadUserDetails(any()) } returns justUser
        coEvery { fakeTokenStore.caid } returns "123"
        coEvery { fakeReleaseDesignConfig.isReleaseDesignActive() } returns true
    }

    @Test
    fun `check new notifications is true with one new follow notification`() {
        val transHistoryPayload = getTenReceivedItems(0)
        val followers = PaginatedFollowers(0, 100, getTenFollowers(1))

        val result = CaffeineResult.Success(followers)
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayload))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationCountViewModel(fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.hasNewNotifications.observeForTesting { event ->
            event.getContentIfNotHandled()?.let { assertTrue(it) }
        }
    }

    @Test
    fun `check new notifications is true with one new received digital item notification`() {
        val transHistoryPayload = getTenReceivedItems(1)
        val followers = PaginatedFollowers(0, 100, getTenFollowers(0))

        val result = CaffeineResult.Success(followers)
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayload))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationCountViewModel(fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.hasNewNotifications.observeForTesting { event ->
            event.getContentIfNotHandled()?.let { assertTrue(it) }
        }
    }

    @Test
    fun `check new notifications is true with one of each notification`() {
        val transHistoryPayload = getTenReceivedItems(1)
        val followers = PaginatedFollowers(0, 100, getTenFollowers(1))

        val result = CaffeineResult.Success(followers)
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayload))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationCountViewModel(fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.hasNewNotifications.observeForTesting { event ->
            event.getContentIfNotHandled()?.let { assertTrue(it) }
        }
    }

    @Test
    fun `check new notifications is true with many of each notification`() {
        val transHistoryPayload = getTenReceivedItems(3)
        val followers = PaginatedFollowers(0, 100, getTenFollowers(4))

        val result = CaffeineResult.Success(followers)
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayload))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationCountViewModel(fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.hasNewNotifications.observeForTesting { event ->
            event.getContentIfNotHandled()?.let { assertTrue(it) }
        }
    }

    @Test
    fun `check new notifications is false with no new notifications`() {
        val transHistoryPayload = getTenReceivedItems(0)
        val followers = PaginatedFollowers(0, 100, getTenFollowers(0))

        val result = CaffeineResult.Success(followers)
        coEvery { fakeUsersRepository.getFollowersList(any()) } returns result
        val transactionHistoryResult = CaffeineResult.Success(PaymentsEnvelope("", 1, transHistoryPayload))
        coEvery { fakeTransactionHistoryRepository.getTransactionHistory() } returns transactionHistoryResult

        subject = NotificationCountViewModel(fakeUsersRepository, fakeTransactionHistoryRepository, fakeFollowManager, fakeTokenStore, fakeReleaseDesignConfig)

        subject.hasNewNotifications.observeForTesting { event ->
            event.getContentIfNotHandled()?.let { assertFalse(it) }
        }
    }

    private fun getTenFollowers(howManyNew: Int): List<CaidRecord.FollowRecord> {
        return (1..10)
            .map {
                val followedAt = if (it <= howManyNew) notificationLastViewed.plusMinutes((it * 2).toLong()) else notificationLastViewed.minusMinutes((it * 2).toLong())
                CaidRecord.FollowRecord("$it", followedAt)
            }
    }

    private fun getTenReceivedItems(howManyNew: Int): TransactionHistoryPayload {
        val list = (1..10)
            .map {
                val createdAt = if (it <= howManyNew) notificationLastViewed.plusMinutes(it * 2L).toEpochSecond().toInt() else notificationLastViewed.minusMinutes(it * 2L).toEpochSecond().toInt()
                HugeTransactionHistoryItem("$it", "ReceiveDigitalItem", createdAt, 1, 1, null, null, 1, null, "", "", "", DigitalItemAssets("", "", ""), null, null)
            }
        return TransactionHistoryPayload(PaymentsCollection(list))
    }
}
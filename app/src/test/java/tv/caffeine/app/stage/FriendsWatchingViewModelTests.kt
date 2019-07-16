package tv.caffeine.app.stage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager

@RunWith(RobolectricTestRunner::class)
class FriendsWatchingViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    companion object {
        const val CAID_1 = "CAID-1"
        const val CAID_2 = "CAID-2"
        const val CAID_3 = "CAID-3"
    }

    @MockK lateinit var mockFollowManager: FollowManager
    @MockK lateinit var mockFriendsWatchingController: FriendsWatchingController
    @MockK(relaxed = true) lateinit var user1: User
    @MockK(relaxed = true) lateinit var user2: User
    @MockK(relaxed = true) lateinit var user3: User
    lateinit var subject: FriendsWatchingViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { mockFollowManager.userDetails(CAID_1) } returns user1
        coEvery { mockFollowManager.userDetails(CAID_2) } returns user2
        coEvery { mockFollowManager.userDetails(CAID_3) } returns user3
        every { user1.caid } returns CAID_1
        every { user2.caid } returns CAID_2
        every { user3.caid } returns CAID_3
        subject = FriendsWatchingViewModel(mockFollowManager, mockFriendsWatchingController)
    }

    @Test
    fun `one friend watching returns list of one`() {
        val flow = flowOf(FriendWatchingEvent(true, CAID_1))
        coEvery { mockFriendsWatchingController.connect(any()) } returns flow
        subject.load("blah")
        subject.friendsWatching.observeForever {
            assertEquals(1, it.size)
        }
    }

    @Test
    fun `two friends watching returns list of two`() {
        val flow = flowOf(
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_2)
        )
        coEvery { mockFriendsWatchingController.connect(any()) } returns flow
        subject.load("blah")
        subject.friendsWatching.observeForever {
            assertEquals(2, it.size)
        }
    }

    @Test
    fun `two friends watching, then one dropping returns list of one`() {
        val flow = flowOf(
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_2),
                FriendWatchingEvent(false, CAID_1)
        )
        coEvery { mockFriendsWatchingController.connect(any()) } returns flow
        subject.load("blah")
        subject.friendsWatching.observeForever {
            assertEquals(1, it.size)
        }
    }

    @Test
    fun `two friends watching, then one dropping and rejoining returns list of two`() {
        val flow = flowOf(
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_2),
                FriendWatchingEvent(false, CAID_1),
                FriendWatchingEvent(true, CAID_1)
        )
        coEvery { mockFriendsWatchingController.connect(any()) } returns flow
        subject.load("blah")
        subject.friendsWatching.observeForever {
            assertEquals(2, it.size)
        }
    }

    @Test
    fun `three friends watching returns list of three`() {
        val flow = flowOf(
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_2),
                FriendWatchingEvent(true, CAID_3)
        )
        coEvery { mockFriendsWatchingController.connect(any()) } returns flow
        subject.load("blah")
        subject.friendsWatching.observeForever {
            assertEquals(3, it.size)
        }
    }

    @Test
    fun `three friends watching, with one friend joining repeatedly, returns list of three`() {
        val flow = flowOf(
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_2),
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_1),
                FriendWatchingEvent(true, CAID_3)
        )
        coEvery { mockFriendsWatchingController.connect(any()) } returns flow
        subject.load("blah")
        subject.friendsWatching.observeForever {
            assertEquals(3, it.size)
        }
    }
}

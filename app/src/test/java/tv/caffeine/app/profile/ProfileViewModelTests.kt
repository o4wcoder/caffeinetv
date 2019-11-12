package tv.caffeine.app.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.test.observeForTesting

@RunWith(RobolectricTestRunner::class)
class ProfileViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true) private lateinit var fakeFollowManager: FollowManager
    @MockK(relaxed = true) private lateinit var fakeUserProfile: UserProfile
    @MockK(relaxed = true) private lateinit var fakeLiveData: LiveData<UserProfile>
    @MockK(relaxed = true) lateinit var fakeUser: User
    @MockK(relaxed = true) lateinit var fakeBroadcastDetails: Broadcast

    lateinit var subject: ProfileViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { fakeLiveData.value } returns fakeUserProfile

        coEvery { fakeFollowManager.loadUserDetails(any()) } returns fakeUser
        coEvery { fakeFollowManager.broadcastDetails(any()) } returns fakeBroadcastDetails
        coEvery { fakeFollowManager.followUser(any(), any()) } returns CaffeineEmptyResult.Success
        coEvery { fakeFollowManager.unfollowUser(any()) } returns CaffeineEmptyResult.Success

        subject = ProfileViewModel(fakeFollowManager)
    }

    @Test
    fun`is following true when user profile is following is true`() {
        val caid = "CAID1"
        every { fakeFollowManager.isFollowing(any()) } returns true
        subject.follow(caid)
        subject.isFollowing.observeForTesting {
            assertTrue(it)
        }
    }

    @Test
    fun`is following false with user profile is following is false`() {
        val caid = "CAID1"
        every { fakeFollowManager.isFollowing(any()) } returns false // mocks server failure up the chain so our "cheat" needs to be reversed
        subject.follow(caid)
        subject.isFollowing.observeForTesting {
            assertFalse(it)
        }
    }

    @Test
    fun`is following false with unfollow server success`() {
        val caid = "CAID1"
        every { fakeFollowManager.isFollowing(any()) } returns false
        subject.follow(caid)
        subject.isFollowing.observeForTesting {
            assertFalse(it)
        }
    }

    @Test
    fun`is following true with unfollow server error`() {
        val caid = "CAID1"
        every { fakeFollowManager.isFollowing(any()) } returns true // mocks server failure up the chain so our "cheat" needs to be reversed
        subject.follow(caid)
        subject.isFollowing.observeForTesting {
            assertTrue(it)
        }
    }
}
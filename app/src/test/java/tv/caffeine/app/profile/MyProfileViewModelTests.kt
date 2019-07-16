package tv.caffeine.app.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager

@RunWith(RobolectricTestRunner::class)
class MyProfileViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: MyProfileViewModel

    companion object {
        private const val FOLLOWING_COUNT = 42
        private const val FOLLOWERS_COUNT = 1000
        private const val FOLLOWERS_COUNT_FORMATTED = "1,000"
        private const val AGE = 99
    }

    private val justUser = User("caid", "username", "name", "email", "avatarImagePath", FOLLOWING_COUNT, FOLLOWERS_COUNT, false, false, "broadcastId", "stageId", mapOf(), mapOf(), AGE, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)

    @Before
    fun setup() {
        val fakeTokenStore = mockk<TokenStore>()
        coEvery { fakeTokenStore.caid } returns "caid"
        val fakeFollowManager = mockk<FollowManager>()
        coEvery { fakeFollowManager.userDetails(any()) } returns justUser
        coEvery { fakeFollowManager.loadUserDetails(any()) } returns justUser
        coEvery { fakeFollowManager.isSelf(any()) } returns false
        coEvery { fakeFollowManager.isFollowing(any()) } returns false
        val fakeUploadAvatarUseCase = mockk<UploadAvatarUseCase>()
        subject = MyProfileViewModel(fakeTokenStore, fakeFollowManager, fakeUploadAvatarUseCase)
    }

    @Test
    fun followersCountIsLoadedCorrectly() {
        assertNotEquals(FOLLOWERS_COUNT, FOLLOWING_COUNT)
        subject.userProfile.observeForever { userProfile ->
            assertEquals(FOLLOWERS_COUNT_FORMATTED, userProfile.followersCount)
        }
    }

    @Test
    fun followingCountIsLoadedCorrectly() {
        subject.userProfile.observeForever { userProfile ->
            assertEquals(FOLLOWING_COUNT.toString(), userProfile.followingCount)
        }
    }
}

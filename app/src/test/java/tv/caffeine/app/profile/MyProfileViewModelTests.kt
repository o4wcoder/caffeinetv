package tv.caffeine.app.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.TestDispatchConfig

class MyProfileViewModelTests {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: MyProfileViewModel

    companion object {
        private const val FOLLOWING_COUNT = 42
        private const val FOLLOWERS_COUNT = 1000
        private const val AGE = 99
    }

    private val justUser = User("caid", "username", "name", "avatarImagePath", FOLLOWING_COUNT, FOLLOWERS_COUNT, false, "broadcastId", "stageId", mapOf(), mapOf(), AGE, "bio", "countryCode", "countryName", "gender", false, false, null)

    @Before
    fun setup() {
        val fakeService = mock<UsersService>()
        val fakeTokenStore = mockk<TokenStore>()
        coEvery { fakeTokenStore.caid } returns "caid"
        val fakeFollowManager = mockk<FollowManager>()
        coEvery { fakeFollowManager.userDetails(any()) } returns justUser
        val fakeUploadAvatarUseCase = mockk<UploadAvatarUseCase>()
        subject = MyProfileViewModel(TestDispatchConfig, fakeTokenStore, fakeFollowManager, fakeUploadAvatarUseCase)
    }

    @Test
    fun followersCountIsLoadedCorrectly() {
        assertNotEquals(FOLLOWERS_COUNT, FOLLOWING_COUNT)
        subject.followersCount.observeForever {
            assertEquals(FOLLOWERS_COUNT.toString(), it)
        }
    }

    @Test
    fun followingCountIsLoadedCorrectly() {
        subject.followingCount.observeForever {
            assertEquals(FOLLOWING_COUNT.toString(), it)
        }
    }

}

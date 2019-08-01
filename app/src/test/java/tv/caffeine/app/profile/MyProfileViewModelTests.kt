package tv.caffeine.app.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.facebook.login.LoginManager
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.SecureSettingsStorage
import tv.caffeine.app.util.CoroutinesTestRule
import tv.caffeine.app.test.observeForTesting

@RunWith(RobolectricTestRunner::class)
class MyProfileViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var subject: MyProfileViewModel
    @MockK private lateinit var fakeAuthWatcher: AuthWatcher
    @MockK private lateinit var fakeFacebookLoginManager: LoginManager
    @MockK private lateinit var fakeAccountsService: AccountsService
    @MockK private lateinit var fakeGson: Gson
    @MockK private lateinit var fakeSecureSettingsStorage: SecureSettingsStorage
    @MockK private lateinit var fakeFollowManager: FollowManager
    @MockK private lateinit var fakeUploadAvatarUseCase: UploadAvatarUseCase
    @MockK(relaxed = true) private lateinit var fakeTokenStore: TokenStore
    @MockK(relaxed = true) private lateinit var fakeFeatureConfig: FeatureConfig
    companion object {
        private const val FOLLOWING_COUNT = 42
        private const val FOLLOWERS_COUNT = 1000
        private const val FOLLOWERS_COUNT_FORMATTED = "1,000"
        private const val AGE = 99
    }

    private val justUser = User("caid", "username", "name", "email", "avatarImagePath", FOLLOWING_COUNT, FOLLOWERS_COUNT, false, false, "broadcastId", "stageId", mapOf(), mapOf(), AGE, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { fakeTokenStore.caid } returns "caid"
        coEvery { fakeFollowManager.userDetails(any()) } returns justUser
        coEvery { fakeFollowManager.loadUserDetails(any()) } returns justUser
        coEvery { fakeFollowManager.isSelf(any()) } returns false
        coEvery { fakeFollowManager.isFollowing(any()) } returns false
        coEvery { fakeSecureSettingsStorage.deviceId } returns "123"
        coEvery { fakeSecureSettingsStorage.deviceId = null } just runs

        val result = CaffeineEmptyResult.Success
        coEvery { fakeAuthWatcher.onSignOut(any()) } returns result
        coEvery { fakeAccountsService.signOut().awaitEmptyAndParseErrors(fakeGson) } returns result

        every { fakeFacebookLoginManager.logOut() } just runs
        every { fakeAccountsService.signOut() } returns mockk()
        subject = MyProfileViewModel(fakeAccountsService, fakeTokenStore, fakeAuthWatcher, fakeFollowManager, fakeUploadAvatarUseCase, fakeFacebookLoginManager, fakeSecureSettingsStorage, fakeFeatureConfig, fakeGson)
    }

    @Test
    fun `followers count is loaded correctly`() {
        assertNotEquals(FOLLOWERS_COUNT, FOLLOWING_COUNT)
        subject.userProfile.observeForTesting { userProfile ->
            assertEquals(FOLLOWERS_COUNT_FORMATTED, userProfile.followersCount)
        }
    }

    @Test
    fun `following count is loaded correctly`() {
        subject.userProfile.observeForTesting { userProfile ->
            assertEquals(FOLLOWING_COUNT.toString(), userProfile.followingCount)
        }
    }

    @Test
    fun `auth watcher sign out is called on sign out`() {
        subject.signOut()
        coVerify(exactly = 1) { fakeAuthWatcher.onSignOut("123") }
    }

    @Test
    fun `facebook login manager log out is called on sign out`() {
        subject.signOut()
        verify(exactly = 1) { fakeFacebookLoginManager.logOut() }
    }

    @Test
    fun `accounts service sign out is called on sign out`() {
        subject.signOut()
        coVerify(exactly = 1) { fakeAccountsService.signOut() }
    }

    @Test
    fun `token store clear gets called on sign out`() {
        subject.signOut()
        coVerify(exactly = 1) { fakeTokenStore.clear() }
    }

    @Test
    fun `feature config clear gets called on sign out`() {
        subject.signOut()
        coVerify(exactly = 1) { fakeFeatureConfig.clear() }
    }

    @Test
    fun `device id is set to null on sign out`() {
        subject.signOut()
        coVerify(exactly = 1) { fakeSecureSettingsStorage.deviceId = null }
    }

    @Test
    fun `signOutComplete is set to true when sign out is finished`() {
        subject.signOut()
        subject.signOutComplete.observeForTesting {
            val signOutComplete = it.peekContent()
            assertNotNull(signOutComplete)
            assertTrue(signOutComplete)
        }
    }
}

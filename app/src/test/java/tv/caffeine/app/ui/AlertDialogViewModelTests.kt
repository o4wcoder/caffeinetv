package tv.caffeine.app.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.model.User
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.CoroutinesTestRule

class AlertDialogViewModelTests {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    @MockK private lateinit var fakeFollowManager: FollowManager
    @MockK private lateinit var fakeAccountRepository: AccountRepository

    private lateinit var subject: AlertDialogViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `is user email verified returns true when email is verified`() {
        val justUser = User("caid", "username", "name", "email", "avatarImagePath", 100, 100,
            false, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false,
            false, null, null, true, false)
        coEvery { fakeFollowManager.loadMyUserDetails() } returns justUser
        subject = AlertDialogViewModel(fakeFollowManager, fakeAccountRepository)
        assertTrue(subject.isUserVerified())
    }

    @Test
    fun `is user email verified returns false when email is not verified`() {
        val justUser = User("caid", "username", "name", "email", "avatarImagePath", 100, 100,
            false, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false,
            false, null, null, false, false)
        coEvery { fakeFollowManager.loadMyUserDetails() } returns justUser
        subject = AlertDialogViewModel(fakeFollowManager, fakeAccountRepository)
        assertFalse(subject.isUserVerified())
    }
}
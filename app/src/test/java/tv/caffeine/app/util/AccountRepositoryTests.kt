package tv.caffeine.app.util

import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.AccountUpdateResult
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.passwordErrorsString
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.session.FollowManager

class AccountRepositoryTests {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    @MockK private lateinit var fakeAccountsService: AccountsService
    @MockK private lateinit var fakeTokenStore: TokenStore
    @MockK private lateinit var fakeFollowManager: FollowManager
    @MockK private lateinit var fakeGson: Gson
    @MockK private lateinit var fakeResources: Resources
    @MockK private lateinit var fakeAccountUpdateResult: AccountUpdateResult
    @MockK private lateinit var fakeCreds: CaffeineCredentials
    @MockK private lateinit var fakeResponse: Deferred<Response<AccountUpdateResult>>

    private lateinit var subject: AccountRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { fakeAccountsService.updateAccount(any()) } returns fakeResponse
        coEvery { fakeResponse.await() } returns Response.success(fakeAccountUpdateResult)
        coEvery { fakeAccountUpdateResult.credentials } returns fakeCreds
        coEvery { fakeTokenStore.storeCredentials(any()) } just runs
        coEvery { fakeFollowManager.loadMyUserDetails() } returns null
        coEvery { fakeResources.getString(any()) } returns "Passwords don't match"

        subject = AccountRepository(fakeAccountsService, fakeTokenStore, fakeFollowManager, fakeResources, fakeGson)
    }

    @Test
    fun `passing mismatched passwords returns error`() = runBlocking {
        val result = runBlocking {
            val currentPassword = ""
            val password1 = "A"
            val password2 = "B"
            Assert.assertNotEquals(password1, password2) // pre-condition
            subject.updatePassword(currentPassword, password1, password2)
        }
        when (result) {
            is CaffeineResult.Error -> Assert.assertNotNull(result.error.passwordErrorsString)
            else -> Assert.fail("Was expecting an error to be returned")
        }
    }

    @Test
    fun `token store store credentials is called on update password success`() {
        runBlocking {
            val currentPassword = ""
            val password1 = "A"
            val password2 = "A"
            Assert.assertEquals(password1, password2) // pre-condition
            subject.updatePassword(currentPassword, password1, password2)
        }
        coVerify(exactly = 1) { fakeTokenStore.storeCredentials(any()) }
    }

    @Test
    fun `follow manager load my user details is called on update email success`() {
        runBlocking {
            subject.updateEmail("password", "email")
        }
        coVerify(exactly = 1) { fakeFollowManager.loadMyUserDetails() }
    }

    @Test
    fun `token store store credentials is called on update email success`() {
        runBlocking {
            subject.updateEmail("password", "email")
        }
        coVerify(exactly = 1) { fakeTokenStore.storeCredentials(any()) }
    }
}
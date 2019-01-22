package tv.caffeine.app.auth

import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.NextAccountAction
import tv.caffeine.app.api.SignInResult

class SignInUseCaseTests {
    @MockK lateinit var gson: Gson
    @MockK lateinit var accountsService: AccountsService
    @MockK lateinit var tokenStore: TokenStore
    @MockK lateinit var authWatcher: AuthWatcher
    @MockK lateinit var signInResponse: Deferred<Response<SignInResult>>
    @MockK lateinit var signInSuccess: SignInResult

    lateinit var subject: SignInUseCase

    companion object {
        const val username = "any"
        const val password = "any"
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = SignInUseCase(gson, accountsService, tokenStore, authWatcher)
        coEvery { accountsService.signIn(any()) } returns signInResponse
        coEvery { signInResponse.await() } returns Response.success(signInSuccess)
        every { tokenStore.storeSignInResult(any()) } just Runs
        every { authWatcher.onSignIn() } just Runs
    }

    @Test
    fun `successful login stores credentials in token store`() {
        every { signInSuccess.next } returns null
        runBlocking { subject(username, password) }
        verify(exactly = 1) { tokenStore.storeSignInResult(any()) }
    }

    @Test
    fun `successful login notifies auth watcher`() {
        every { signInSuccess.next } returns null
        runBlocking { subject(username, password) }
        verify(exactly = 1) { authWatcher.onSignIn() }
    }

    @Test
    fun `login that requires MFA does not notify auth watcher`() {
        every { signInSuccess.next } returns NextAccountAction.mfa_otp_required
        runBlocking { subject(username, password) }
        verify(exactly = 0) { authWatcher.onSignIn() }
        verify(exactly = 0) { tokenStore.storeSignInResult(any()) }
    }

}

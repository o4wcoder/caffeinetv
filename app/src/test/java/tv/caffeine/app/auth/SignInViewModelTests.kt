package tv.caffeine.app.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.NextAccountAction
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.util.TestDispatchConfig

class SignInViewModelTests {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: SignInViewModel
    @MockK lateinit var signInUseCase: SignInUseCase

    companion object {
        private val emptyCredentials = CaffeineCredentials("", "", "", "")
        private fun resultWithNextAction(next: NextAccountAction) = CaffeineResult.Success(
                SignInResult("", "", emptyCredentials, "", next, null))

        val mfaRequired = resultWithNextAction(NextAccountAction.mfa_otp_required)
        val mustAcceptToc = resultWithNextAction(NextAccountAction.legal_acceptance_required)
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = SignInViewModel(TestDispatchConfig, signInUseCase)
    }

    @Test
    fun `accounts with two factor auth return MFARequired`() {
        coEvery { signInUseCase.invoke(any(), any()) } returns mfaRequired
        subject.login("", "")
        subject.signInOutcome.observeForever {
            assertEquals(SignInOutcome.MFARequired, it)
        }
    }

    @Test
    fun `old accounts must accept terms`() {
        coEvery { signInUseCase.invoke(any(), any()) } returns mustAcceptToc
        subject.login("", "")
        subject.signInOutcome.observeForever {
            assertEquals(SignInOutcome.MustAcceptTerms, it)
        }
    }

}

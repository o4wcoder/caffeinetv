package tv.caffeine.app.login

import android.view.View
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.just
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.repository.AccountRepository

@RunWith(RobolectricTestRunner::class)
class WelcomeViewModelTests {

    private lateinit var subject: WelcomeViewModel
    @MockK
    lateinit var accountRepository: AccountRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = WelcomeViewModel(accountRepository)
        subject.email = "test@email.com"
    }

    @Test
    fun `test resend email link is visible if it has never been clicked`() {
        assertEquals(subject.getResendEmailVisibility(), View.VISIBLE)
    }

    @Test
    fun `test sent email is not visible if resend email link has never been clicked`() {
        assertEquals(subject.getEmailDisplayVisibility(), View.INVISIBLE)
    }

    @Test
    fun `test resend email link is no longer visible once clicked`() {
        coEvery { accountRepository.resendVerification() } just Runs
        subject.onResendEmailClick()
        assertEquals(subject.getResendEmailVisibility(), View.INVISIBLE)
    }

    @Test
    fun `test email address display when resend email isclicked`() {
        coEvery { accountRepository.resendVerification() } just Runs
        subject.onResendEmailClick()
        assertEquals(subject.getEmailDisplayVisibility(), View.VISIBLE)
    }
}
package tv.caffeine.app.login

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.ConfirmEmailResponse
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.makeGenericUser

@RunWith(RobolectricTestRunner::class)
class ConfirmEmailViewModelTests {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK private lateinit var accountRepository: AccountRepository
    @MockK private lateinit var followManager: FollowManager
    @MockK private lateinit var confirmEmailResponse: ConfirmEmailResponse

    private lateinit var subject: ConfirmEmailViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { followManager.loadMyUserDetails() } returns makeGenericUser()
    }

    @Test
    fun `loading indicator visibile before load`() {
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        assertTrue(subject.getLoadingVisibility() == View.VISIBLE)
    }

    @Test
    fun `title text visible before load`() {
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        assertTrue(subject.getTitleVisiblity() == View.VISIBLE)
    }

    @Test
    fun `success layout not visible before load`() {
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        assertTrue(subject.getSuccessVisibility() == View.GONE)
    }

    @Test
    fun `subtitle not visible before load`() {
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        assertTrue(subject.getSubtitleVisibility() == View.GONE)
    }

    @Test
    fun `email confirmation text view not visible before load`() {
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        assertTrue(subject.getEmailConfirmationVisibility() == View.GONE)
    }

    @Test
    fun `button not visible before load`() {
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        assertTrue(subject.getButtonVisibility() == View.GONE)
    }

    @Test
    fun `title text correct before load`() {
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        assertEquals(subject.getTitleText(), R.string.confirming_your_account)
    }

    @Test
    fun `on success hides loading indicator`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getLoadingVisibility() == View.GONE)
    }

    @Test
    fun `on success hides title`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getTitleVisiblity() == View.GONE)
    }

    @Test
    fun `on success shows success layout`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getSuccessVisibility() == View.VISIBLE)
    }

    @Test
    fun `on success hides subtitle textview`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getSubtitleVisibility() == View.GONE)
    }

    @Test
    fun `on success hides email confirmation textview`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getEmailConfirmationVisibility() == View.GONE)
    }

    @Test
    fun `on success shows button`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getButtonVisibility() == View.VISIBLE)
    }

    @Test
    fun `on success button text correct`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertEquals(subject.getButtonText(), R.string.lets_go)
    }

    @Test
    fun `on success button is enabled`() {
        val success = CaffeineResult.Success(confirmEmailResponse)
        coEvery { accountRepository.confirmEmail(any()) } returns success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.isButtonEnabled)
    }

    @Test
    fun `on failure hides loading indicator`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getLoadingVisibility() == View.GONE)
    }

    @Test
    fun `on failure shows title`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getTitleVisiblity() == View.VISIBLE)
    }

    @Test
    fun `on failure shows correct title text`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertEquals(subject.getTitleText(), R.string.somethings_not_right)
    }

    @Test
    fun `on failure hides success layout`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getSuccessVisibility() == View.GONE)
    }

    @Test
    fun `on failure shows subtitle textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getSubtitleVisibility() == View.VISIBLE)
    }

    @Test
    fun `on failure shows correct subtitle text`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertEquals(subject.getSubtitleText(), R.string.we_couldnt_confirm_your_account)
    }

    @Test
    fun `on failure hides email confirmation textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getEmailConfirmationVisibility() == View.GONE)
    }

    @Test
    fun `on failure shows button`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.getButtonVisibility() == View.VISIBLE)
    }

    @Test
    fun `on failure button text is correct`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertEquals(subject.getButtonText(), R.string.resend_email)
    }

    @Test
    fun `on failure button is enabled`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertTrue(subject.isButtonEnabled)
    }

    @Test
    fun `on failure follow manager load my user details is called`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        coVerify(exactly = 1) { followManager.loadMyUserDetails() }
    }

    @Test
    fun `on failure user email is correct`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        assertEquals(subject.userEmailAddress, "email")
    }

    @Test
    fun `on failure resend email success hides loading indicator`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getLoadingVisibility() == View.GONE)
    }

    @Test
    fun `on failure resend email success hides success layout`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getSuccessVisibility() == View.GONE)
    }

    @Test
    fun `on failure resend email success shows title textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getTitleVisiblity() == View.VISIBLE)
    }

    @Test
    fun `on failure resend email success shows correct title text`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertEquals(subject.getTitleText(), R.string.somethings_not_right)
    }

    @Test
    fun `on failure resend email success shows subtitle textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getSubtitleVisibility() == View.VISIBLE)
    }

    @Test
    fun `on failure resend email success shows correct subtitle text`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertEquals(subject.getSubtitleText(), R.string.we_couldnt_confirm_your_account)
    }

    @Test
    fun `on failure resend email success shows email confirmation textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getEmailConfirmationVisibility() == View.VISIBLE)
    }

    @Test
    fun `on failure resend email success shows button`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getButtonVisibility() == View.VISIBLE)
    }

    @Test
    fun `on failure resend email success button not enabled`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertFalse(subject.isButtonEnabled)
    }

    @Test
    fun `on failure resend email success button text correct`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Success
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertEquals(subject.getButtonText(), R.string.resend_email)
    }

    @Test
    fun `on failure resend email account repository resend verification is called`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns mockk()
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        coVerify(exactly = 1) { accountRepository.resendVerification() }
    }

    @Test
    fun `on failure resend email failure hides loading indicator`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getLoadingVisibility() == View.GONE)
    }

    @Test
    fun `on failure resend email failure hides success layout`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getSuccessVisibility() == View.GONE)
    }

    @Test
    fun `on failure resend email failure shows title textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getTitleVisiblity() == View.VISIBLE)
    }

    @Test
    fun `on failure resend email failure shows correct title text`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertEquals(subject.getTitleText(), R.string.somethings_not_right)
    }

    @Test
    fun `on failure resend email failure shows subtitle textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getSubtitleVisibility() == View.VISIBLE)
    }

    @Test
    fun `on failure resend email failure shows correct subtitle text`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertEquals(subject.getSubtitleText(), R.string.could_not_send_email)
    }

    @Test
    fun `on failure resend email failure hides email confirmation textview`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getEmailConfirmationVisibility() == View.GONE)
    }

    @Test
    fun `on failure resend email failure shows button`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertTrue(subject.getButtonVisibility() == View.VISIBLE)
    }

    @Test
    fun `on failure resend email failure button not enabled`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertFalse(subject.isButtonEnabled)
    }

    @Test
    fun `on failure resend email failure button text correct`() {
        val error = CaffeineResult.Error<ApiErrorResult>(ApiErrorResult(null)) as CaffeineResult<ConfirmEmailResponse>
        coEvery { accountRepository.confirmEmail(any()) } returns error
        coEvery { accountRepository.resendVerification() } returns CaffeineEmptyResult.Error(
            ApiErrorResult(null)
        )
        subject = ConfirmEmailViewModel(accountRepository, followManager)
        subject.load("", "")
        subject.resendEmail()
        assertEquals(subject.getButtonText(), R.string.resend_email)
    }
}
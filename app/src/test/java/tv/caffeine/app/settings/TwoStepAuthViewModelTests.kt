package tv.caffeine.app.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.repository.TwoStepAuthRepository
import tv.caffeine.app.settings.authentication.TwoStepAuthViewModel
import tv.caffeine.app.test.observeForTesting

@RunWith(RobolectricTestRunner::class)
class TwoStepAuthViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: TwoStepAuthViewModel
    @MockK lateinit var mockTwoStepAuthRepository: TwoStepAuthRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = TwoStepAuthViewModel(mockTwoStepAuthRepository)
    }

    @Test
    fun `verify mfaEnabled live data returns false if set to false`() {
        subject.updateMfaEnabled(false)
        subject.mfaEnabledUpdate.observeForTesting { event ->
            event.getContentIfNotHandled()?.let { result ->
                assertFalse(result)
            }
        }
    }

    @Test
    fun `verify mfaEnabled live data returns true if set to true`() {
        subject.updateMfaEnabled(true)
        subject.mfaEnabledUpdate.observeForTesting { event ->
            val result = event.getContentIfNotHandled()
            assertNotNull(result)
            requireNotNull(result)
            assertTrue(result)
        }
    }

    @Test
    fun `success in disabling mfa will turn off mta livedata`() {
        coEvery { mockTwoStepAuthRepository.disableAuth() } returns CaffeineEmptyResult.Success
        subject.disableAuth()
        subject.mfaEnabledUpdate.observeForTesting { event ->
            val result = event.getContentIfNotHandled()
            assertNotNull(result)
            requireNotNull(result)
            assertFalse(result)
        }
    }

    @Test
    fun `verification code button is disabled when verification code size is zero`() {
        subject.onVerificationCodeTextChanged("")
        assertFalse(subject.isVerificationCodeButtonEnabled())
    }

    @Test
    fun `verification code button is enabled when verification code size is not zero`() {
        subject.onVerificationCodeTextChanged("123456")
        assertTrue(subject.isVerificationCodeButtonEnabled())
    }

    @Test
    fun `verify that verification button click calls send verification code on repository`() {
        coEvery { mockTwoStepAuthRepository.sendVerificationCode(any()) } returns CaffeineEmptyResult.Success
        subject.onVerificationCodeButtonClick()
        coVerify(exactly = 1) { mockTwoStepAuthRepository.sendVerificationCode(any()) }
    }

    @Test
    fun `verify text input on verification code change enable status of button`() {
        subject.onVerificationCodeTextChanged("")
        assertFalse(subject.isVerificationCodeButtonEnabled())
        subject.onVerificationCodeTextChanged("1")
        assertTrue(subject.isVerificationCodeButtonEnabled())
        subject.onVerificationCodeTextChanged("12")
        assertTrue(subject.isVerificationCodeButtonEnabled())
        subject.onVerificationCodeTextChanged("1")
        assertTrue(subject.isVerificationCodeButtonEnabled())
        subject.onVerificationCodeTextChanged("")
        assertFalse(subject.isVerificationCodeButtonEnabled())
    }

    @Test
    fun `verification code button is disabled when verification code size is zero after setting code directly`() {
        subject.setVerificationCode("")
        assertFalse(subject.isVerificationCodeButtonEnabled())
    }

    @Test
    fun `verification code button is enabled when verification code size is not zero after setting code directly`() {
        subject.setVerificationCode("123456")
        assertTrue(subject.isVerificationCodeButtonEnabled())
    }
}
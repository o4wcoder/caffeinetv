package tv.caffeine.app.login

import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResetPasswordViewModelTests {

    private lateinit var subject: ResetPasswordViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = ResetPasswordViewModel(mockk())
    }

    @Test
    fun `when passwords match validatePasswords returns true`() {
        subject.password = "password"
        subject.confirmPassword = "password"
        assertTrue(subject.validatePasswords())
    }

    @Test
    fun `when passwords do not match validatPasswords returns false`() {
        subject.password = "password"
        subject.confirmPassword = "wrongpassword"
        assertFalse(subject.validatePasswords())
    }

    @Test
    fun `when both password fields contain text reset password button is enabled`() {
        subject.password = "password"
        subject.confirmPassword = "password"
        assertTrue(subject.isResetPasswordButtonEnabled())
    }

    @Test
    fun `when password field is empty reset password button is not enabled`() {
        subject.password = ""
        subject.confirmPassword = "password"
        assertFalse(subject.isResetPasswordButtonEnabled())
    }

    @Test
    fun `when confirm password field is empty reset password button is not enabled`() {
        subject.password = "password"
        subject.confirmPassword = ""
        assertFalse(subject.isResetPasswordButtonEnabled())
    }

    @Test
    fun `when both password fields are empty reset password button is not enabled`() {
        subject.password = ""
        subject.confirmPassword = ""
        assertFalse(subject.isResetPasswordButtonEnabled())
    }
}
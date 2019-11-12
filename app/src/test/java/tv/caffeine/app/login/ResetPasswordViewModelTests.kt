package tv.caffeine.app.login

import android.view.View
import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.junit.Assert.assertEquals
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
    fun `when passwords match no error is shown`() {
        subject.password = "password"
        subject.confirmPassword = "password"
        subject.validatePasswords()
        assertEquals(subject.getErrorTextVisibility(), View.INVISIBLE)
    }

    @Test
    fun `when passwords do not match error is shown`() {
        subject.password = "password"
        subject.confirmPassword = "wrongpassword"
        subject.validatePasswords()
        assertEquals(subject.getErrorTextVisibility(), View.VISIBLE)
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
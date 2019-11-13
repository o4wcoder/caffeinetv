package tv.caffeine.app.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignUpViewModelTests {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: SignUpViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = SignUpViewModel(mockk())
        subject.email = "test@email.com"
        subject.username = "caffeineuser"
        subject.password = "password"
        subject.birthdate = "1/1/1970"
        subject.passwordIsValid = true
    }

    @Test
    fun `test sign up button is enabled if all fields contain data and password is valid`() {
        assertTrue(subject.isSignUpButtonEnabled())
    }

    @Test
    fun `test sign up button is disabled if all fields contain data and password is not valid`() {
        subject.passwordIsValid = false
        assertFalse(subject.isSignUpButtonEnabled())
    }

    @Test
    fun `test sign up button is disabled if email is missing`() {
        subject.email = ""
        assertFalse(subject.isSignUpButtonEnabled())
    }

    @Test
    fun `test sign up button is disabled if username is missing`() {
        subject.username = ""
        assertFalse(subject.isSignUpButtonEnabled())
    }

    @Test
    fun `test sign up button is disabled if birthdate is missing`() {
        subject.birthdate = ""
        assertFalse(subject.isSignUpButtonEnabled())
    }
}
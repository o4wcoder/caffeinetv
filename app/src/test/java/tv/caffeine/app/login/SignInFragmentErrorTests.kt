package tv.caffeine.app.login

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector

@RunWith(RobolectricTestRunner::class)
class SignInFragmentErrorTests {

    private lateinit var subject: SignInFragment

    @MockK
    lateinit var navController: NavController

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        every { navController.navigate(any<NavDirections>()) } just Runs
        val scenario = launchFragmentInContainer {
            SignInFragment(mockk()).also {
                it.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                    if (viewLifecycleOwner != null) {
                        // The fragment’s view has just been created
                        Navigation.setViewNavController(it.requireView(), navController)
                    }
                }
            }
        }
        scenario.onFragment {
            subject = it
        }
    }

    @Test
    fun `verify username edit text loses focus when there is an error`() {
        subject.binding.usernameEditTextLayout.requestFocus()
        subject.binding.usernameEditTextLayout.text = "username"
        assertTrue(subject.binding.usernameEditTextLayout.hasFocus())
        subject.onSignInError()
        assertFalse(subject.binding.usernameEditTextLayout.hasFocus())
    }

    @Test
    fun `verify password edit text loses focus when there is an error`() {
        subject.binding.passwordEditTextLayout.requestFocus()
        subject.binding.passwordEditTextLayout.text = "password"
        assertTrue(subject.binding.passwordEditTextLayout.hasFocus())
        subject.onSignInError()
        assertFalse(subject.binding.passwordEditTextLayout.hasFocus())
    }

    @Test
    fun `verify log in button is disabled when there is not text in username or password`() {
        subject.binding.usernameEditTextLayout.text = ""
        subject.binding.passwordEditTextLayout.text = ""
        assertFalse(subject.binding.signInButton.isEnabled)
    }

    @Test
    fun `verify log in button is disabled when there is just text in username and not password`() {
        subject.binding.usernameEditTextLayout.text = "username"
        subject.binding.passwordEditTextLayout.text = ""
        assertFalse(subject.binding.signInButton.isEnabled)
    }

    @Test
    fun `verify log in button is disabled when there is just text in the passwaor and not username`() {
        subject.binding.usernameEditTextLayout.text = ""
        subject.binding.passwordEditTextLayout.text = "password"
        assertFalse(subject.binding.signInButton.isEnabled)
    }

    @Test
    fun `verify log in button is enabled when there is text in username or password`() {
        subject.binding.usernameEditTextLayout.text = "username"
        subject.binding.passwordEditTextLayout.text = "password"
        assertTrue(subject.binding.signInButton.isEnabled)
    }
}
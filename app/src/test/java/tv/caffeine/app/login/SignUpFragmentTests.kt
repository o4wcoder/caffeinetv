package tv.caffeine.app.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector

private inline fun launchSignUpFragmentWithArgs(
    username: String? = null,
    email: String? = null,
    iid: String? = null,
    showErrorText: Boolean = false,
    crossinline action: (SignUpFragment) -> Unit
) {
    val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
    val testComponent = DaggerTestComponent.factory().create(app)
    app.setApplicationInjector(testComponent)
    val arguments = SignUpFragmentArgs(username, email, iid, showErrorText).toBundle()
    val navController = mockk<NavController>(relaxed = true)
    val scenario = launchFragmentInContainer(arguments, R.style.AppTheme) {
        SignUpFragment(mockk(), mockk(), mockk()).also {
            it.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    // The fragment’s view has just been created
                    Navigation.setViewNavController(it.requireView(), navController)
                }
            }
        }
    }
    scenario.onFragment {
        action(it)
    }
}

@RunWith(RobolectricTestRunner::class)
class SignUpFragmentTests {

    private val TITLE_TEXT = "Let's get\nstarted"
    private val TITLE_TEXT_FROM_SOCIAL_ERROR = "Something went wrong. Try email?"
    private val BUTTON_TEXT = "sign up"
    private val BUTTON_TEXT_FROM_SOCIAL_ERROR = "let's go"

    private lateinit var fragment: SignUpFragment

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `title text is correct when arriving from button click`() {
        launchSignUpFragmentWithArgs {
            fragment = it
        }
        assertEquals(TITLE_TEXT, fragment.binding.signUpTitleText.text)
    }

    @Test
    fun `title text is correct when arriving from social failure`() {
        launchSignUpFragmentWithArgs(showErrorText = true) {
            fragment = it
        }
        assertEquals(TITLE_TEXT_FROM_SOCIAL_ERROR, fragment.binding.signUpTitleText.text)
    }

    @Test
    fun `button text is correct when arriving from button click`() {
        launchSignUpFragmentWithArgs {
            fragment = it
        }
        assertEquals(BUTTON_TEXT, fragment.binding.signUpButton.text)
    }

    @Test
    fun `button text is correct when arriving from social failure`() {
        launchSignUpFragmentWithArgs(showErrorText = true) {
            fragment = it
        }
        assertEquals(BUTTON_TEXT_FROM_SOCIAL_ERROR, fragment.binding.signUpButton.text)
    }
}
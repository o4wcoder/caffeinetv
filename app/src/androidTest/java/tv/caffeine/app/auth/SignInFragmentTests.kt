package tv.caffeine.app.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.core.StringContains.containsString
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.BaseNavigationTest
import tv.caffeine.app.R
import tv.caffeine.app.util.navigateToLanding
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SignInFragmentTests : BaseNavigationTest() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun prep() {
            IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.SECONDS)
            IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
        }

        const val invalidUsernameOrPasswordErrorMessage = "The username or password provided is incorrect."
        const val invalidUsername = "definitely_wrong_username"
        const val invalidPassword = "absolutely_incorrect_password"
    }

    override fun navigateToTestDestination() {
        navController.navigateToLanding()
        onView(withId(R.id.sign_in_with_email_button)).perform(click())
    }

    @Test
    override fun testDestination() {
        onView(withId(R.id.username_edit_text)).check(matches(isDisplayed()))
    }

    @Test
    fun invalidLoginShowsErrorMessage() {
        onView(withId(R.id.username_edit_text)).perform(click(), typeText(invalidUsername))
        onView(withId(R.id.password_edit_text)).perform(click(), typeText(invalidPassword))
        onView(withId(R.id.sign_in_button)).perform(click())
        // TODO - wait for login to complete
        onView(withId(R.id.form_error_text_view)).check(matches(withText(containsString(invalidUsernameOrPasswordErrorMessage))))
    }

    @Test
    fun missingUsernameShowsErrorMessage() {
        onView(withId(R.id.password_edit_text)).perform(click(), typeText(invalidPassword))
        onView(withId(R.id.sign_in_button)).perform(click())
        // TODO - wait for login to complete
        onView(withId(R.id.form_error_text_view)).check(matches(withText(containsString(invalidUsernameOrPasswordErrorMessage))))
    }

    @Test
    fun missingPasswordShowsErrorMessage() {
        onView(withId(R.id.username_edit_text)).perform(click(), typeText(invalidUsername))
        onView(withId(R.id.sign_in_button)).perform(click())
        // TODO - wait for login to complete
        onView(withId(R.id.form_error_text_view)).check(matches(withText(containsString(invalidUsernameOrPasswordErrorMessage))))
    }

}

package tv.caffeine.app

import android.content.Intent
import androidx.annotation.IdRes
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.get
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NavigationTests {
    private lateinit var navigator: FragmentNavigator

    @Before
    fun prep() {
        val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)
        val mainActivity = activityTestRule.launchActivity(Intent())
        val fragmentManager = mainActivity.supportFragmentManager
        val navHostFragment = fragmentManager.primaryNavigationFragment as NavHostFragment
        navigator = navHostFragment.navController.navigatorProvider[FragmentNavigator::class]
    }

    @Test
    fun signInWithEmailButtonNavigatesToTheSignInFragment() {
        check(click = R.id.sign_in_with_email_button, destination = R.id.signInFragment)
    }

    @Test
    fun newAccountButtonNavigatesToTheSignUpFragment() {
        check(click = R.id.new_account_button, destination = R.id.signUpFragment)
    }

    private fun check(@IdRes click: Int, @IdRes destination: Int) {
        val countDownLatch = CountDownLatch(1)
        navigator.addOnNavigatorNavigatedListener { _, destId, _ ->
            assertEquals(destId, destination)
            countDownLatch.countDown()
        }
        onView(withId(click)).perform(click())
        countDownLatch.await(1, TimeUnit.SECONDS)
    }
}

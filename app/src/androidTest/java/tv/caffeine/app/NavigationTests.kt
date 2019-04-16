package tv.caffeine.app

import android.content.Intent
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.get
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//@RunWith(AndroidJUnit4::class)
class NavigationTests {
    private lateinit var navigator: FragmentNavigator
    private lateinit var navController: NavController

//    @Before
    fun prep() {
        val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)
        val mainActivity = activityTestRule.launchActivity(Intent())
        val fragmentManager = mainActivity.supportFragmentManager
        val navHostFragment = fragmentManager.primaryNavigationFragment as NavHostFragment
        navController = navHostFragment.navController
        navigator = navController.navigatorProvider[FragmentNavigator::class]
    }

//    @Test
    fun signInWithEmailButtonNavigatesToTheSignInFragment() {
        check(click = R.id.sign_in_with_username_text_view, destination = R.id.signInFragment)
    }

//    @Test
    fun newAccountButtonNavigatesToTheSignUpFragment() {
        check(click = R.id.new_account_button, destination = R.id.signUpFragment)
    }

    private fun check(@IdRes click: Int, @IdRes destination: Int) {
        val countDownLatch = CountDownLatch(1)
        navController.addOnDestinationChangedListener { _, actualDestination, _ ->
            assertEquals(actualDestination.id, destination)
            countDownLatch.countDown()
        }
        onView(withId(click)).perform(click())
        countDownLatch.await(1, TimeUnit.SECONDS)
    }
}

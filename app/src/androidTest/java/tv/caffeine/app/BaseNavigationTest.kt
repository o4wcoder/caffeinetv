package tv.caffeine.app

import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseNavigationTest {
    protected lateinit var navController: NavController
    @get:Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setup() {
        val mainActivity = activityTestRule.activity
        val fragmentManager = mainActivity.supportFragmentManager
        val navHostFragment = fragmentManager.primaryNavigationFragment as NavHostFragment
        navController = navHostFragment.navController
        navigateToTestDestination()
    }

    abstract fun navigateToTestDestination()

    @Test
    abstract fun testDestination()

}

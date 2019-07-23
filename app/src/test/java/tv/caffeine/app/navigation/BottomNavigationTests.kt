package tv.caffeine.app.navigation

import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.InjectionActivityTestRule

@RunWith(RobolectricTestRunner::class)
class BottomNavigationTests {

    private val activityTestRule = InjectionActivityTestRule(MainActivity::class.java, DaggerTestComponent.factory())
    private lateinit var mainActivity: MainActivity

    @Before
    fun setup() {
        mainActivity = activityTestRule.launchActivity(Intent())
    }

    @After
    fun cleanup() {
        activityTestRule.finishActivity()
    }

    @Test
    fun `the star bottom navigation tab is selected when the current screen is lobby`() {
        val bottomNavigationView = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.isSelected = false
        mainActivity.updateBottomNavigationStatus(bottomNavigationView, R.id.lobbySwipeFragment)
        assertTrue(bottomNavigationView.menu.findItem(R.id.bottom_nav_star_menu_item).isChecked)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_flame_menu_item).isChecked)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_clock_menu_item).isChecked)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_profile_menu_item).isChecked)
    }

    @Test
    fun `the clock bottom navigation tab is selected when the current screen is FPG`() {
        val bottomNavigationView = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.isSelected = false
        mainActivity.updateBottomNavigationStatus(bottomNavigationView, R.id.featuredProgramGuideFragment)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_star_menu_item).isChecked)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_flame_menu_item).isChecked)
        assertTrue(bottomNavigationView.menu.findItem(R.id.bottom_nav_clock_menu_item).isChecked)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_profile_menu_item).isChecked)
    }

    @Test
    fun `the profile bottom navigation tab is selected when the current screen is my profile`() {
        val bottomNavigationView = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.isSelected = false
        mainActivity.updateBottomNavigationStatus(bottomNavigationView, R.id.myProfileFragment)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_star_menu_item).isChecked)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_flame_menu_item).isChecked)
        assertFalse(bottomNavigationView.menu.findItem(R.id.bottom_nav_clock_menu_item).isChecked)
        assertTrue(bottomNavigationView.menu.findItem(R.id.bottom_nav_profile_menu_item).isChecked)
    }
}
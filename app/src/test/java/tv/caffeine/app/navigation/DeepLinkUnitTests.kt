package tv.caffeine.app.navigation

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build.VERSION_CODES.O_MR1
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.findDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.get
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.InjectionActivityTestRule

/**
 * https://github.com/robolectric/robolectric/issues/3698
 * DeepLinkUnitTests.setup() will fail with SDK 28 and Robolectric 4.2. SDK 27 works.
 * TODO: AND-140 to try the latest Robolectric library.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [O_MR1])
class DeepLinkUnitTests(private val url: String, @IdRes private val destination: Int) {
    private lateinit var navController: NavController
    private lateinit var resources: Resources
    private val activityTestRule = InjectionActivityTestRule(MainActivity::class.java, DaggerTestComponent.factory())

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data() = listOf(
                arrayOf("https://www.caffeine.tv/account/claim-gold/index.html?id=token", R.id.caffeineLinksFragment),
                arrayOf("https://www.caffeine.tv/account/reset-password/?code=code", R.id.caffeineLinksFragment),
                arrayOf("https://www.caffeine.tv/account/reset-password?code=code", R.id.caffeineLinksFragment),
                arrayOf("https://www.caffeine.tv/account/email-confirmation/index.html", R.id.confirmEmailFragment),
                arrayOf("https://www.caffeine.tv/account/email-confirmation/index.html?code=code&caid=caid", R.id.confirmEmailFragment),
                arrayOf("https://www.caffeine.tv/FoxSports", R.id.stagePagerFragment),
                arrayOf("https://www.caffeine.tv/FoxSports/profile", R.id.profileFragment),
                arrayOf("https://www.caffeine.tv/caffeine", R.id.stagePagerFragment),
                arrayOf("https://www.caffeine.tv/caffeine?bst=sharing", R.id.stagePagerFragment),
                arrayOf("https://www.caffeine.tv/caffeine/profile", R.id.profileFragment),
                arrayOf("https://www.caffeine.tv/privacy.html", R.id.caffeineLinksFragment),
                arrayOf("https://www.caffeine.tv/tos.html", R.id.caffeineLinksFragment)
        )
    }

    @Before
    fun setup() {
        val mainActivity = activityTestRule.launchActivity(Intent())
        val fragmentManager = mainActivity.supportFragmentManager
        val navHostFragment = fragmentManager.primaryNavigationFragment as NavHostFragment
        navController = navHostFragment.navController
        resources = navHostFragment.resources
    }

    @After
    fun cleanup() {
        activityTestRule.finishActivity()
    }

    @Test
    fun `verify deep link`() {
        val navGraph = navController.graph
        val uri = Uri.parse(url)
        val expectedDestination = navController.graph[destination]
        val actualDestination = navGraph.findDestination(uri)
        assertNotNull("unexpected null", actualDestination)
        requireNotNull(actualDestination)
        val expectedLabel = expectedDestination.label
        val actualLabel = actualDestination.label
        val expectedResource = resources.getResourceEntryName(expectedDestination.id)
        val actualResource = resources.getResourceEntryName(actualDestination.id)
        assertEquals("Deep link: $url; Destinations: Expected: $expectedLabel, actual: $actualLabel", expectedResource, actualResource)
    }
}

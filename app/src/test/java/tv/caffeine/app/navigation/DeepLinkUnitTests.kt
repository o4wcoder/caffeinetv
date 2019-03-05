package tv.caffeine.app.navigation

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.findDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.get
import androidx.test.annotation.UiThreadTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.InjectionActivityTestRule

@RunWith(AndroidJUnit4::class)
class DeepLinkUnitTests {
    private lateinit var navController: NavController
    private lateinit var resources: Resources

    private val expectedDeepLinkDestinations: List<Pair<String, Int>> = listOf(
            "https://www.caffeine.tv/account/claim-gold/index.html?id=token" to R.id.caffeineLinksFragment,
            "https://www.caffeine.tv/account/reset-password/?code=code" to R.id.caffeineLinksFragment,
            "https://www.caffeine.tv/account/reset-password?code=code" to R.id.caffeineLinksFragment,
            "https://www.caffeine.tv/account/email-confirmation/index.html" to R.id.confirmEmailFragment,
            "https://www.caffeine.tv/account/email-confirmation/index.html?code=code&caid=caid" to R.id.confirmEmailFragment,
            "https://www.caffeine.tv/ESL" to R.id.stageFragment,
            "https://www.caffeine.tv/ESL/profile" to R.id.profileFragment,
            "https://www.caffeine.tv/caffeine" to R.id.stageFragment,
            "https://www.caffeine.tv/caffeine?bst=sharing" to R.id.stageFragment,
            "https://www.caffeine.tv/caffeine/profile" to R.id.profileFragment,
            "https://www.caffeine.tv/privacy.html" to R.id.caffeineLinksFragment,
            "https://www.caffeine.tv/tos.html" to R.id.caffeineLinksFragment
    )

    @Before
    fun setup() {
        val activityTestRule = InjectionActivityTestRule(MainActivity::class.java, DaggerTestComponent.builder())
        val mainActivity = activityTestRule.launchActivity(Intent())
        val fragmentManager = mainActivity.supportFragmentManager
        val navHostFragment = fragmentManager.primaryNavigationFragment as NavHostFragment
        navController = navHostFragment.navController
        resources = navHostFragment.resources
    }

    @Test
    @UiThreadTest
    fun `check all links`() {
        val navGraph = navController.graph
        for ((url, destination) in expectedDeepLinkDestinations) {
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

}

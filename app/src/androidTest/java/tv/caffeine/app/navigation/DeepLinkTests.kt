package tv.caffeine.app.navigation

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.get
import androidx.test.annotation.UiThreadTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DeepLinkTests {
    private lateinit var navController: NavController
    private lateinit var resources: Resources

    @DataPoints
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
        val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)
        val mainActivity = activityTestRule.launchActivity(Intent())
        val fragmentManager = mainActivity.supportFragmentManager
        val navHostFragment = fragmentManager.primaryNavigationFragment as NavHostFragment
        navController = navHostFragment.navController
        resources = navHostFragment.resources
    }

    @Test
    @UiThreadTest
    fun tosLinkIsHandled() {
        val result = navigateToDeepLink("https://www.caffeine.tv/tos.html")
        assertTrue(result)
    }

    @Test
    @UiThreadTest
    fun allLinks() {
        for (linkAndDestination in expectedDeepLinkDestinations) {
            deepLinksNavigateCorrectly(linkAndDestination)
            val url = linkAndDestination.first
            val expectedDestination = navController.graph[linkAndDestination.second]
            deepLinkNavigatesCorrectly(url, expectedDestination)
        }
    }

    @Theory
    @UiThreadTest
    fun deepLinksNavigateCorrectly(linkAndDestination: Pair<String, Int>) {
        val url = linkAndDestination.first
        val expectedDestination = navController.graph[linkAndDestination.second]
        deepLinkNavigatesCorrectly(url, expectedDestination)
    }

    private fun deepLinkNavigatesCorrectly(url: String, expectedDestination: NavDestination) {
        navigateToDeepLink(url) { actualDestination, _ ->
            val expectedLabel = expectedDestination.label
            val actualLabel = actualDestination.label
            val expectedResource = resources.getResourceEntryName(expectedDestination.id)
            val actualResource = resources.getResourceEntryName(actualDestination.id)
            assertEquals("Deep link: $url; Destinations: Expected: $expectedLabel, actual: $actualLabel", expectedResource, actualResource)
        }
    }

    private fun navigateToDeepLink(url: String, block: (NavDestination, Bundle?) -> Unit) {
        val result = navigateToDeepLink(url)
        val countDownLatch = CountDownLatch(1)
        val listener = NavController.OnDestinationChangedListener { _, actualDestination, arguments ->
            block(actualDestination, arguments)
            countDownLatch.countDown()
        }
        navController.addOnDestinationChangedListener(listener)
        navController.removeOnDestinationChangedListener(listener)
        countDownLatch.await(1, TimeUnit.SECONDS)
    }

    private fun navigateToDeepLink(url: String): Boolean {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return navController.handleDeepLink(intent)
    }
}

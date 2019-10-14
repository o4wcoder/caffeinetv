package tv.caffeine.app.analytics

import com.google.gson.Gson
import com.kochava.base.Tracker
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R

@RunWith(RobolectricTestRunner::class)
class KochavaAnalyticsTests {

    private lateinit var analytics: KochavaAnalytics
    @MockK lateinit var configuration: Tracker.Configuration

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        analytics = KochavaAnalytics(configuration, Gson())
    }

    @Test
    fun `stage navigation is created if it is included in the deferred deeplink`() {
        analytics.handleAttribution(buildAttribution(true, "stage", "user"))
        analytics.handleDeferredDeeplink { navDirections ->
            assertEquals(R.id.action_global_stagePagerFragment, navDirections?.actionId)
            assertEquals("user", navDirections?.arguments?.get("broadcastLink"))
        }
    }

    @Test
    fun `stage navigation is created and returned only once if it is included in the deferred deeplink`() {
        analytics.handleAttribution(buildAttribution(true, "stage", "user"))
        analytics.handleDeferredDeeplink { assertNotNull(it) }
        analytics.handleDeferredDeeplink { assertNull(it) }
    }

    @Test
    fun `stage navigation is not created if the page included in the deferred deeplink is incorrect or null`() {
        analytics.handleAttribution(buildAttribution(true, "wrongPage", "user"))
        analytics.handleDeferredDeeplink { assertNull(it) }
        analytics.handleAttribution(buildAttribution(true, "user", null))
        analytics.handleDeferredDeeplink { assertNull(it) }
    }

    @Test
    fun `stage navigation is not created if the username included in the deferred deeplink is null`() {
        analytics.handleAttribution(buildAttribution(true, "stage", null))
        analytics.handleDeferredDeeplink { assertNull(it) }
    }

    @Test
    fun `stage navigation is not created if the attribution included in the deferred deeplink is false or null`() {
        analytics.handleAttribution(buildAttribution(false, null, null))
        analytics.handleDeferredDeeplink { assertNull(it) }
        analytics.handleAttribution(buildAttribution(null, null, null))
        analytics.handleDeferredDeeplink { assertNull(it) }
    }

    private fun buildAttribution(hasAttribution: Boolean?, page: String?, user: String?): String {
        val params = listOfNotNull(
            hasAttribution?.let { "\"attribution\":\"$it\"" },
            page?.let { "\"page\":\"$it\"" },
            user?.let { "\"user\":\"$it\"" }
        ).joinToString()
        return "{$params}"
    }
}
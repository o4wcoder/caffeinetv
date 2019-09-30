package tv.caffeine.app.domain

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.BuildConfig
import tv.caffeine.app.net.AppMetaDataInterceptor

@RunWith(RobolectricTestRunner::class)
class AppMetaDataInterceptorTests {

    private lateinit var context: Context
    private lateinit var subject: AppMetaDataInterceptor

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        subject = AppMetaDataInterceptor(context)
    }

    @Test
    fun `the user agent, client type, and client version are set correctly`() {
        val chain = AuthorizationInterceptorTests.TestChain()
        subject.intercept(chain)
        val request = chain.requestToProceed
        val versionCode = subject.versionCode
        val versionName = BuildConfig.VERSION_NAME
        val sdkVersion = Build.VERSION.SDK_INT
        assertEquals("Caffeine/$versionCode Android/$sdkVersion", request?.header("User-Agent"))
        assertEquals("android", request?.header("X-Client-Type"))
        assertEquals(versionName, request?.header("X-Client-Version"))
    }
}

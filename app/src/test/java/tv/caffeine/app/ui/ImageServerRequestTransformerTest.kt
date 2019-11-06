package tv.caffeine.app.ui

import android.net.Uri
import com.squareup.picasso.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.settings.InMemorySettingsStorage

@RunWith(RobolectricTestRunner::class)
class ImageServerRequestTransformerTest {
    lateinit var subject: ImageServerRequestTransformer

    @Before
    fun setup() {
        val serverConfig = ServerConfig(InMemorySettingsStorage(environment = null))
        subject = ImageServerRequestTransformer(serverConfig)
    }

    @Test
    fun `assets requests are transformed`() {
        val request = makeRequest("https://assets.caffeine.tv/random.png")
        val result = subject.transformRequest(request)
        assertFalse(result === request)
    }

    @Test
    fun `images requests are transformed`() {
        val request = makeRequest("https://images.caffeine.tv/random.png")
        val result = subject.transformRequest(request)
        assertFalse(result === request)
    }

    @Test
    fun `non images or assets requests are not transformed`() {
        val request = makeRequest("https://api.caffeine.tv/random.png")
        val result = subject.transformRequest(request)
        assertTrue(result === request)
    }

    @Test
    fun `client side resize is removed on transformed requests`() {
        val request = makeRequest("https://assets.caffeine.tv/random.png", 100, 100)
        assertTrue(request.hasSize())
        val result = subject.transformRequest(request)
        assertFalse(result.hasSize())
    }

    @Test
    fun `client side resize is not removed on non-transformed requests`() {
        val request = makeRequest("https://api.caffeine.tv/random.png", 100, 100)
        assertTrue(request.hasSize())
        val result = subject.transformRequest(request)
        assertTrue(result.hasSize())
    }

    private fun makeRequest(uriString: String) = Request.Builder(Uri.parse(uriString)).build()

    private fun makeRequest(uriString: String, width: Int, height: Int) = Request.Builder(Uri.parse(uriString))
            .resize(width, height)
            .build()
}

@RunWith(RobolectricTestRunner::class)
class ImageServerTests {

    @Test
    fun `assets requests are processed via fastly`() {
        val serverConfig = ServerConfig(InMemorySettingsStorage(environment = null))
        val uri = Uri.parse("https://assets.caffeine.tv/random.png")
        val imageServer = ImageServer.Factory.makeRequestBuilder(uri, serverConfig)
        assertTrue(imageServer is ImageServer.Fastly)
    }

    @Test
    fun `images requests are processed via fastly`() {
        val serverConfig = ServerConfig(InMemorySettingsStorage(environment = null))
        val uri = Uri.parse("https://images.caffeine.tv/random.png")
        val imageServer = ImageServer.Factory.makeRequestBuilder(uri, serverConfig)
        assertTrue(imageServer is ImageServer.Fastly)
    }

    @Test
    fun `requests other than production images or assets are not processed by an image server`() {
        val serverConfig = ServerConfig(InMemorySettingsStorage(environment = null))
        val uri = Uri.parse("https://api.caffeine.tv/random.png")
        val imageServer = ImageServer.Factory.makeRequestBuilder(uri, serverConfig)
        assertNull(imageServer)
    }
}

@RunWith(RobolectricTestRunner::class)
class FastlyTests {

    @Test
    fun `requests without width and height do not include the respective parameters`() {
        val subject = ImageServer.Fastly(Uri.parse("https://assets.caffeine.tv/random.png"))
        val uri = subject.buildUri()
        assertFalse(uri.queryParameterNames.contains("width"))
        assertFalse(uri.queryParameterNames.contains("height"))
    }

    @Test
    fun `requests with width and height include the respective parameters`() {
        val subject = ImageServer.Fastly(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.resize(1, 1)
        val uri = subject.buildUri()
        assertTrue(uri.queryParameterNames.contains("width"))
        assertTrue(uri.queryParameterNames.contains("height"))
    }

    @Test
    fun `requests with centerInside include the fit parameter`() {
        val subject = ImageServer.Fastly(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.centerInside = true
        val uri = subject.buildUri()
        assertTrue(uri.queryParameterNames.contains("fit"))
        assertEquals("bounds", uri.getQueryParameter("fit"))
    }

    @Test
    fun `requests with centerCrop include the fit parameter`() {
        val subject = ImageServer.Fastly(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.centerCrop = true
        val uri = subject.buildUri()
        assertTrue(uri.queryParameterNames.contains("fit"))
        assertEquals("crop", uri.getQueryParameter("fit"))
    }

    @Test
    fun `requests with resize create correct uri`() {
        val subject = ImageServer.Fastly(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.resize(1, 1)
        val uri = subject.buildUri()
        assertEquals("https://assets.caffeine.tv/random.png?optimize=true&width=1&height=1", uri.toString())
    }

    @Test
    fun `requests with centerInside create correct uri`() {
        val subject = ImageServer.Fastly(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.resize(1, 1)
        subject.centerInside = true
        val uri = subject.buildUri()
        assertEquals("https://assets.caffeine.tv/random.png?optimize=true&fit=bounds&width=1&height=1", uri.toString())
    }

    @Test
    fun `requests with centerCrop create correct uri`() {
        val subject = ImageServer.Fastly(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.resize(1, 1)
        subject.centerCrop = true
        val uri = subject.buildUri()
        assertEquals("https://assets.caffeine.tv/random.png?optimize=true&fit=crop&width=1&height=1", uri.toString())
    }
}

@RunWith(RobolectricTestRunner::class)
class ImgixTests {

    @Test
    fun `requests without width and height do not include the respective parameters`() {
        val subject = ImageServer.Imgix(Uri.parse("https://assets.caffeine.tv/random.png"))
        val uri = subject.buildUri()
        assertFalse(uri.queryParameterNames.contains("w"))
        assertFalse(uri.queryParameterNames.contains("h"))
    }

    @Test
    fun `requests with width and height include the respective parameters`() {
        val subject = ImageServer.Imgix(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.resize(1, 1)
        val uri = subject.buildUri()
        assertTrue(uri.queryParameterNames.contains("w"))
        assertTrue(uri.queryParameterNames.contains("h"))
    }

    @Test
    fun `requests with resize include the auto and fit parameters`() {
        val subject = ImageServer.Imgix(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.resize(1, 1)
        val uri = subject.buildUri()
        assertTrue(uri.queryParameterNames.contains("auto"))
        assertTrue(uri.queryParameterNames.contains("fit"))
    }

    @Test
    fun `requests with resize create correct uri`() {
        val subject = ImageServer.Imgix(Uri.parse("https://assets.caffeine.tv/random.png"))
        subject.resize(1, 1)
        val uri = subject.buildUri()
        assertEquals("https://assets.caffeine.tv/random.png?auto=compress&fit=clip&w=1&h=1", uri.toString())
    }
}

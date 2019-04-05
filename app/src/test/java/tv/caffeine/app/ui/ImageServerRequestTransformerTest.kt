package tv.caffeine.app.ui

import android.net.Uri
import com.squareup.picasso.Request
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageServerRequestTransformerTest {

    @Test
    fun `assets requests are transformed`() {
        val request = makeRequest("https://assets.caffeine.tv/random.png")
        val subject = ImageServerRequestTransformer()
        val result = subject.transformRequest(request)
        assertFalse(result === request)
    }

    @Test
    fun `images requests are transformed`() {
        val request = makeRequest("https://images.caffeine.tv/random.png")
        val subject = ImageServerRequestTransformer()
        val result = subject.transformRequest(request)
        assertFalse(result === request)
    }

    @Test
    fun `non images or assets requests are not transformed`() {
        val request = makeRequest("https://api.caffeine.tv/random.png")
        val subject = ImageServerRequestTransformer()
        val result = subject.transformRequest(request)
        assertTrue(result === request)
    }

    @Test
    fun `client side resize is removed on transformed requests`() {
        val request = makeRequest("https://assets.caffeine.tv/random.png", 100, 100)
        assertTrue(request.hasSize())
        val subject = ImageServerRequestTransformer()
        val result = subject.transformRequest(request)
        assertFalse(result.hasSize())
    }

    @Test
    fun `client side resize is not removed on non-transformed requests`() {
        val request = makeRequest("https://api.caffeine.tv/random.png", 100, 100)
        assertTrue(request.hasSize())
        val subject = ImageServerRequestTransformer()
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
        val imageServer = ImageServer.Factory.makeRequestBuilder(Uri.parse("https://assets.caffeine.tv/random.png"))
        assertTrue(imageServer is ImageServer.Fastly)
    }

    @Test
    fun `images requests are processed via imgix`() {
        val imageServer = ImageServer.Factory.makeRequestBuilder(Uri.parse("https://images.caffeine.tv/random.png"))
        assertTrue(imageServer is ImageServer.Imgix)
    }

    @Test
    fun `requests other than production images or assets are not processed by an image server`() {
        val imageServer = ImageServer.Factory.makeRequestBuilder(Uri.parse("https://api.caffeine.tv/random.png"))
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

}

package tv.caffeine.app.net

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.di.ASSETS_BASE_URL
import tv.caffeine.app.di.IMAGES_BASE_URL
import tv.caffeine.app.settings.InMemorySettingsStorage

@RunWith(RobolectricTestRunner::class)
class ServerConfigTest {

    @Test
    fun `leaves prod environment assets url alone`() {
        val subject = ServerConfig(InMemorySettingsStorage(environment = null))
        val uri = Uri.parse("$ASSETS_BASE_URL/random.png")
        val result = subject.normalizeImageUri(uri)
        assertEquals("https://assets.caffeine.tv/random.png", result.toString())
    }

    @Test
    fun `normalizes dev environment assets url`() {
        val subject = ServerConfig(InMemorySettingsStorage(environment = "random-environment"))
        val uri = Uri.parse("$ASSETS_BASE_URL/random.png")
        val result = subject.normalizeImageUri(uri)
        assertEquals("https://assets.random-environment.caffeine.tv/random.png", result.toString())
    }

    @Test
    fun `leaves prod environment images url alone`() {
        val subject = ServerConfig(InMemorySettingsStorage(environment = null))
        val uri = Uri.parse("$IMAGES_BASE_URL/random.png")
        val result = subject.normalizeImageUri(uri)
        assertEquals("https://images.caffeine.tv/random.png", result.toString())
    }

    @Test
    fun `normalizes dev environment images url`() {
        val subject = ServerConfig(InMemorySettingsStorage(environment = "random-environment"))
        val uri = Uri.parse("$IMAGES_BASE_URL/random.png")
        val result = subject.normalizeImageUri(uri)
        assertEquals("https://images.random-environment.caffeine.tv/random.png", result.toString())
    }

    @Test
    fun `leaves non-images or non-assets urls alone`() {
        val subject = ServerConfig(InMemorySettingsStorage(environment = "random-environment"))
        val uri = Uri.parse("https://api.random-environment.caffeine.tv/random.png")
        val result = subject.normalizeImageUri(uri)
        assertEquals("https://api.random-environment.caffeine.tv/random.png", result.toString())
    }

}

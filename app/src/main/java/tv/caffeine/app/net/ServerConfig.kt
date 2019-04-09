package tv.caffeine.app.net

import android.net.Uri
import tv.caffeine.app.di.ASSETS_BASE_URL
import tv.caffeine.app.di.IMAGES_BASE_URL
import tv.caffeine.app.settings.SettingsStorage
import javax.inject.Inject

const val PRODUCTION = "production"
const val STAGING = "staging"

class ServerConfig @Inject constructor(settingsStorage: SettingsStorage) {
    private val environment = settingsStorage.environment
    private val modifier = when {
        environment == null || environment.isBlank() || environment == PRODUCTION -> ""
        else -> ".$environment"
    }
    val api = "https://api$modifier.caffeine.tv"
    val realtime = "https://realtime$modifier.caffeine.tv"
    val realtimeWebSocket = "wss://realtime$modifier.caffeine.tv"
    val payments = "https://payments$modifier.caffeine.tv"
    val events = "https://events$modifier.caffeine.tv"
    val assets = "https://assets$modifier.caffeine.tv"
    val images = "https://images$modifier.caffeine.tv"

    fun normalizeImageUri(uri: Uri): Uri {
        val stringUrl = uri.toString()
        val normalizedUrl = normalizeImageUrl(stringUrl)
        return Uri.parse(normalizedUrl)
    }

    private fun normalizeImageUrl(url: String) = when {
        url.startsWith(ASSETS_BASE_URL) -> url.replaceFirst(ASSETS_BASE_URL, assets)
        url.startsWith(IMAGES_BASE_URL) -> url.replaceFirst(IMAGES_BASE_URL, images)
        else -> url
    }
}

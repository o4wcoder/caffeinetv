package tv.caffeine.app.auth

import okhttp3.Request
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.RefreshTokenBody
import tv.caffeine.app.api.RefreshTokenResult
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.settings.SettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
        private val settingsStorage: SettingsStorage
) {
    private var accessToken: String? = null
    private var credential: String? = null
    var caid: String? = null
        private set

    fun storeSignInResult(signInResult: SignInResult) {
        settingsStorage.refreshToken = signInResult.refreshToken
        caid = signInResult.caid
        accessToken = signInResult.accessToken
        credential = signInResult.credentials.credential
    }

    fun storeCredentials(credentials: CaffeineCredentials) {
        settingsStorage.refreshToken = credentials.refreshToken
        caid = credentials.caid
        accessToken = credentials.accessToken
        credential = credentials.credential
    }

    fun storeRefreshTokenResult(refreshTokenResult: RefreshTokenResult) {
        storeCredentials(refreshTokenResult.credentials)
    }

    fun clear() {
        settingsStorage.refreshToken = null
        caid = null
        accessToken = null
        credential = null
    }

    fun createRefreshTokenBody() = settingsStorage.refreshToken?.let { RefreshTokenBody(it) }

    fun webSocketHeader() = """{
                "Headers": {
                    "x-credential" : "${credential ?: ""}",
                    "authorization" : "Bearer ${accessToken ?: ""}",
                    "X-Client-Type" : "android",
                    "X-Client-Version" : "0"
                }
            }""".trimMargin()

    fun addHttpHeaders(requestBuilder: Request.Builder) {
        requestBuilder.apply {
            accessToken?.let { header("Authorization", "Bearer $it") }
            credential?.let { header("X-Credential", it) }
        }
    }

}

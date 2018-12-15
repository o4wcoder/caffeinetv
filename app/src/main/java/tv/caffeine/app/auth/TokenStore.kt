package tv.caffeine.app.auth

import okhttp3.Request
import tv.caffeine.app.api.*
import tv.caffeine.app.settings.SettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
        private val settingsStorage: SettingsStorage
) {
    private var accessToken: String? = null
    private var credential: String? = null
    private var _caid: String? = null
    var caid: String?
        get() = _caid ?: settingsStorage.caid?.apply { _caid = this }
        set(value) {
            _caid = value
            settingsStorage.caid = value
        }

    fun storeSignInResult(signInResult: SignInResult) {
        if (signInResult.next != NextAccountAction.legal_acceptance_required) {
            settingsStorage.refreshToken = signInResult.refreshToken
        }
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
        if (refreshTokenResult.next != NextAccountAction.legal_acceptance_required) {
            settingsStorage.refreshToken = refreshTokenResult.credentials.refreshToken
        }
        caid = refreshTokenResult.credentials.caid
        accessToken = refreshTokenResult.credentials.accessToken
        credential = refreshTokenResult.credentials.credential
    }

    fun clear() {
        settingsStorage.refreshToken = null
        caid = null
        accessToken = null
        credential = null
    }

    fun createRefreshTokenBody() = settingsStorage.refreshToken?.let { RefreshTokenBody(it) }

    fun header() = """
                "Headers": {
                    "x-credential" : "${credential ?: ""}",
                    "authorization" : "Bearer ${accessToken ?: ""}",
                    "X-Client-Type" : "android",
                    "X-Client-Version" : "0"
                }
            """.trimMargin()

    fun webSocketHeader() = "{${header()}}".trimMargin()

    fun webSocketHeaderAndSignedPayload(payload: String) = """{
                ${header()},
                "Body": "{\"user\":\"$payload\"}"
            }""".trimMargin()

    fun addHttpHeaders(requestBuilder: Request.Builder) {
        requestBuilder.apply {
            accessToken?.let { header("Authorization", "Bearer $it") }
            credential?.let { header("X-Credential", it) }
        }
    }

}

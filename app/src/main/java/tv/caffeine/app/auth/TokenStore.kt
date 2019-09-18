package tv.caffeine.app.auth

import androidx.annotation.VisibleForTesting
import com.crashlytics.android.Crashlytics
import okhttp3.Request
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.NextAccountAction
import tv.caffeine.app.api.RefreshTokenBody
import tv.caffeine.app.api.RefreshTokenResult
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.settings.SecureSettingsStorage
import tv.caffeine.app.settings.SettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    private val settingsStorage: SettingsStorage,
    private val secureSettingsStorage: SecureSettingsStorage
) {
    @VisibleForTesting var accessToken: String? = null
    @VisibleForTesting var credential: String? = null
    private var _caid: CAID? = null
    var caid: CAID?
        get() = _caid ?: settingsStorage.caid?.apply { _caid = this }
        set(value) {
            _caid = value
            settingsStorage.caid = value
            Crashlytics.setUserIdentifier(value)
        }

    val hasRefreshToken: Boolean get() = settingsStorage.refreshToken != null

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
        settingsStorage.clearCredentials()
        secureSettingsStorage.clear()
        caid = null
        accessToken = null
        credential = null
    }

    fun createRefreshTokenBody() = settingsStorage.refreshToken?.let { RefreshTokenBody(it) }

    fun credential() = credential ?: ""

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
            header("X-Client-Type", "android")
        }
    }
}

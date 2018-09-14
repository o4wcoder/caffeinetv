package tv.caffeine.app.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import okhttp3.Request
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.RefreshTokenBody
import tv.caffeine.app.api.RefreshTokenResult
import tv.caffeine.app.api.SignInResult

class TokenStore(private val sharedPreferences: SharedPreferences) {
    private var refreshToken: String?
        get() = sharedPreferences.getString("REFRESH_TOKEN", null)
        set(value) = sharedPreferences.edit {
            if (value == null) {
                remove("REFRESH_TOKEN")
            } else {
                putString("REFRESH_TOKEN", value)
            }
        }
    private var accessToken: String? = null
    private var credential: String? = null
    var caid: String? = null
    private set

    fun storeSignInResult(signInResult: SignInResult) {
        refreshToken = signInResult.refreshToken
        caid = signInResult.caid
        accessToken = signInResult.accessToken
        credential = signInResult.credentials.credential
    }

    fun storeCredentials(credentials: CaffeineCredentials) {
        refreshToken = credentials.refreshToken
        caid = credentials.caid
        accessToken = credentials.accessToken
        credential = credentials.credential
    }

    fun storeRefreshTokenResult(refreshTokenResult: RefreshTokenResult) {
        storeCredentials(refreshTokenResult.credentials)
    }

    fun clear() {
        refreshToken = null
        caid = null
        accessToken = null
        credential = null
    }

    fun createRefreshTokenBody() = refreshToken?.let { RefreshTokenBody(it) }

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

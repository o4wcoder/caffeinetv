package tv.caffeine.app.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.RefreshTokenResult
import tv.caffeine.app.api.SignInResult

class TokenStore(private val sharedPreferences: SharedPreferences) {
    var refreshToken: String?
        get() = sharedPreferences.getString("REFRESH_TOKEN", null)
        set(value) = sharedPreferences.edit {
            if (value == null) {
                remove("REFRESH_TOKEN")
            } else {
                putString("REFRESH_TOKEN", value)
            }
        }
    var accessToken: String? = null
    var credential: String? = null

    fun storeSignInResult(signInResult: SignInResult) {
        refreshToken = signInResult.refreshToken
        accessToken = signInResult.accessToken
        credential = signInResult.credentials.credential
    }

    fun storeCredentials(credentials: CaffeineCredentials) {
        refreshToken = credentials.refreshToken
        accessToken = credentials.accessToken
        credential = credentials.credential
    }

    fun storeRefreshTokenResult(refreshTokenResult: RefreshTokenResult) {
        refreshToken = refreshTokenResult.credentials.refreshToken
        accessToken = refreshTokenResult.credentials.accessToken
        credential = refreshTokenResult.credentials.credential
    }

    fun clear() {
        refreshToken = null
        accessToken = null
        credential = null
    }
}

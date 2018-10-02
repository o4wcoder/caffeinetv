package tv.caffeine.app.net

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import tv.caffeine.app.api.RefreshTokenService
import tv.caffeine.app.auth.TokenStore

class TokenAuthenticator(private val refreshTokenService: RefreshTokenService, private val tokenStore: TokenStore) : Authenticator {
    override fun authenticate(route: Route?, response: Response?): Request? {
        if (response == null) return null
        val refreshTokenBody = tokenStore.createRefreshTokenBody() ?: return null

        val result = refreshTokenService.refreshTokenSync(refreshTokenBody).execute()
        if (!result.isSuccessful) return null
        val credentials = result.body()?.credentials ?: return null

        tokenStore.storeCredentials(credentials)
        return response.request().newBuilder()
                .apply { tokenStore.addHttpHeaders(this) }
                .build()
    }

}

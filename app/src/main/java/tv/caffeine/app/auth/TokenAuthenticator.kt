package tv.caffeine.app.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import tv.caffeine.app.api.RefreshTokenService

class TokenAuthenticator(private val refreshTokenService: RefreshTokenService, private val tokenStore: TokenStore) : Authenticator {
    override fun authenticate(route: Route?, response: Response?): Request? {
        if (response == null || !shouldAttemptAuthentication(response)) return null
        val refreshTokenBody = tokenStore.createRefreshTokenBody() ?: return null

        val result = refreshTokenService.refreshTokenSync(refreshTokenBody).execute()
        if (!result.isSuccessful) return null
        val credentials = result.body()?.credentials ?: return null

        tokenStore.storeCredentials(credentials)
        return response.request().newBuilder()
                .apply { tokenStore.addHttpHeaders(this) }
                .build()
    }

    private fun shouldAttemptAuthentication(response: Response): Boolean {
        return !tokenStore.alreadyContainsAuthorization(response) && response.responseCount() <= 3
    }

}

fun Response.responseCount(): Int {
    val response = priorResponse()
    return if (response == null) 1 else 1 + response.responseCount()
}

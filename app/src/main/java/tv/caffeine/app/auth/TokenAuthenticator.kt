package tv.caffeine.app.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

class TokenAuthenticator(private val refreshTokenService: RefreshTokenService, private val tokenStore: TokenStore) : Authenticator {
    override fun authenticate(route: Route?, response: Response?): Request? {
        val response = response ?: return null
        val refreshToken = tokenStore.refreshToken ?: return null
        val refreshTokenBody = RefreshTokenBody(refreshToken)

        val result = refreshTokenService.refreshTokenSync(refreshTokenBody).execute()
        if (!result.isSuccessful) return null
        val yay = result.body() ?: return null

        val credentials = yay.credentials
        tokenStore.refreshToken = credentials.refreshToken
        tokenStore.accessToken = credentials.accessToken
        tokenStore.credential = credentials.credential
        return response.request().newBuilder()
                .header("Authorization", "Bearer ${credentials.accessToken}")
                .header("X-Credential", "Bearer ${credentials.credential}")
                .build()
    }

}

interface RefreshTokenService {
    @POST("v1/account/token")
    fun refreshTokenSync(@Body refreshTokenBody: RefreshTokenBody): Call<RefreshTokenResult>
}

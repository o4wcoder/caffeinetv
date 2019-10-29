package tv.caffeine.app.api

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.IdentityProvider

interface OAuthService {
    @GET("v1/oauth/{provider}/url")
    fun authenticateWith(@Path("provider") provider: IdentityProvider): Deferred<Response<CaffeineOAuthResponse>>

    @POST("auth/callback")
    fun callback(@Query("state") state: String)

    @POST("v1/oauth/facebook/callback")
    fun submitFacebookToken(@Body body: FacebookTokenBody): Deferred<Response<OAuthCallbackResult>>

    @GET
    @Headers("Long-poll: true")
    fun longPoll(@Url url: String): Deferred<Response<OAuthCallbackResult>>
}

class CaffeineOAuthResponse(val authUrl: String, val longpollUrl: String)

class FacebookTokenBody(val token: String)

class OAuthCallbackResult(val retryIn: Int?, val oauth: OAuthDetails?, val possibleUsername: String?, val redirectUrl: String?, val signUpPageUrl: String?, val next: NextAccountAction?, val mfaOtpMethod: String?, val caid: CAID?, val loginToken: String?, val errors: Map<String, List<String>>?, val credentials: CaffeineCredentials?)

fun OAuthCallbackResult.shouldRetryRequest() =
    retryIn != null
        && loginToken == null
        && oauth == null
        && credentials == null
        && next == null

@Parcelize
data class OAuthDetails(val uid: String, val provider: IdentityProvider, val displayName: String, val username: String?, val email: String?, val iid: String) : Parcelable

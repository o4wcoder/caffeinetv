package tv.caffeine.app.api

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Deferred
import retrofit2.http.*
import tv.caffeine.app.api.model.IdentityProvider

interface OAuthService {
    @GET("v1/oauth/{provider}/url")
    fun authenticateWith(@Path("provider") provider: IdentityProvider): Deferred<CaffeineOAuthResponse>

    @POST("auth/callback")
    fun callback(@Query("state") state: String)

    @POST("v1/oauth/facebook/callback")
    fun submitFacebookToken(@Body body: FacebookTokenBody): Deferred<OAuthCallbackResult>
}

class CaffeineOAuthResponse(val authUrl: String, val longpollUrl: String)

class FacebookTokenBody(val token: String)

@Parcelize
class OAuthCallbackResult(val oauth: OAuthDetails?, val possibleUsername: String?, val redirectUrl: String?, val signUpPageUrl: String?, val next: String?, val mfaOtpMethod: String?, val caid: String?, val loginToken: String?, val errors: Map<String, List<String>>?): Parcelable

@Parcelize
data class OAuthDetails(val uid: String, val provider: IdentityProvider, val displayName: String, val username: String?, val email: String?, val iid: String) : Parcelable

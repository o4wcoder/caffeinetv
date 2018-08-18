package tv.caffeine.app.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface Accounts {
    @POST("v1/account/signin")
    fun signin(@Body signInBody: SignInBody): Call<SignInResult>

    @POST("v1/account/token")
    fun refreshToken(@Body refreshTokenBody: RefreshTokenBody): Call<RefreshTokenResult>
}

class SignInBody(val account: Account)
class Account(val username: String, val password: String)

class SignInResult(val accessToken: String, val caid: String, val credentials: CaffeineCredentials, val refreshToken: String)

class RefreshTokenBody(val refreshToken: String)
class RefreshTokenResult(val credentials: CaffeineCredentials, val next: String)

class CaffeineCredentials(val accessToken: String, val caid: String, val credential: String, val refreshToken: String)

class ApiErrorResult(val errors: ApiError)

class ApiError(val _error: Array<String>, val username: Array<String>, val password: Array<String>)

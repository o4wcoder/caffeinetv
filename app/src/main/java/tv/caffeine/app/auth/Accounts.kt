package tv.caffeine.app.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface Accounts {
    @POST("v1/account/signin")
    fun signin(@Body signInBody: SignInBody): Call<SignInResult>

    @POST("v1/account/token")
    fun refreshToken(@Body refreshTokenBody: RefreshTokenBody): Call<RefreshTokenResult>

    @POST("v1/account/forgot-password")
    fun forgotPassword(@Body forgotPasswordBody: ForgotPasswordBody): Call<Void>

    @DELETE("v1/account/token")
    fun signOut(): Call<Unit>

    @POST("v1/account")
    fun signUp(@Body signUpBody: SignUpBody): Call<SignUpResult>
}

class SignInBody(val account: Account)
class Account(val username: String, val password: String)

class SignInResult(val accessToken: String, val caid: String, val credentials: CaffeineCredentials, val refreshToken: String)

class RefreshTokenBody(val refreshToken: String)
class RefreshTokenResult(val credentials: CaffeineCredentials, val next: String)

class CaffeineCredentials(val accessToken: String, val caid: String, val credential: String, val refreshToken: String)

class ApiErrorResult(val errors: ApiError)

class ApiError(val _error: Array<String>, val username: Array<String>, val password: Array<String>)

class ForgotPasswordBody(val email: String)

class SignUpBody(val account: SignUpAccount, val iid: String?, val tos: Boolean)
class SignUpAccount(val username: String, val password: String, val email: String, val dob: String, val countryCode: String)
class SignUpResult(val credentials: CaffeineCredentials, val next: String)

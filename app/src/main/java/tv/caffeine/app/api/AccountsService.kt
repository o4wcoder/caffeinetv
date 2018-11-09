package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface AccountsService {
    @POST("v1/account/signin")
    fun signIn(@Body signInBody: SignInBody): Deferred<Response<SignInResult>>

    @POST("v1/account/forgot-password")
    fun forgotPassword(@Body forgotPasswordBody: ForgotPasswordBody): Deferred<Response<Void>>

    @DELETE("v1/account/token")
    fun signOut(): Call<Unit>

    @POST("v1/account")
    fun signUp(@Body signUpBody: SignUpBody): Deferred<Response<SignUpResult>>

    @PATCH("v1/account")
    fun updateAccount(@Body body: UpdateAccountBody): Deferred<Response<AccountUpdateResult>>

    @Multipart
    @PUT("v1/upload/avatar")
    fun uploadAvatar(@Part("avatar\"; filename=\"avatar.png") image: RequestBody): Deferred<Response<UploadAvatarResult>>

    @GET("v1/account/notification-settings")
    fun getNotificationSettings(): Deferred<Response<NotificationSettings>>

    @PATCH("v1/account/notification-settings")
    fun updateNotificationSettings(@Body notificationSettings: NotificationSettings): Deferred<Response<NotificationSettings>>
}

class SignInBody(val account: Account, val mfa: MfaCode? = null)
class Account(val username: String? = null, val password: String? = null, val caid: String? = null, val loginToken: String? = null)

class MfaCode(val otp: String)

class SignInResult(val accessToken: String, val caid: String, val credentials: CaffeineCredentials, val refreshToken: String, val next: NextAccountAction?, val mfaOtpMethod: String?)

class RefreshTokenBody(val refreshToken: String)
class RefreshTokenResult(val credentials: CaffeineCredentials, val next: NextAccountAction)

class CaffeineCredentials(val accessToken: String, val caid: String, val credential: String, val refreshToken: String)

class ApiErrorResult(val errors: ApiError)

fun ApiErrorResult.isTokenExpirationError() = !errors._token.isNullOrEmpty()

class ApiError(
        val _error: Array<String>? = null,
        val username: Array<String>? = null,
        val password: Array<String>? = null,
        val currentPassword: Array<String>? = null,
        val email: Array<String>? = null,
        val otp: Array<String>? = null,
        val _token: Array<String>? = null
)

class ForgotPasswordBody(val email: String)

class SignUpBody(val account: SignUpAccount, val iid: String?, val tos: Boolean)
class SignUpAccount(val username: String, val password: String, val email: String, val dob: String, val countryCode: String)
class SignUpResult(val credentials: CaffeineCredentials, val next: NextAccountAction)

class UpdateAccountBody(val account: AccountUpdateRequest)
class AccountUpdateRequest(val currentPassword: String, val password: String? = null, val email: String? = null)
class AccountUpdateResult(val credentials: CaffeineCredentials, val next: NextAccountAction?)

class UploadAvatarResult(val avatarImagePath: String)
val UploadAvatarResult.avatarImageUrl: String get() = "https://images.caffeine.tv$avatarImagePath"

// omits iOS and web settings
class NotificationSettings(
        val newFollowerEmail: Boolean? = null,
        val weeklySuggestionsEmail: Boolean? = null,
        val broadcastLiveEmail: Boolean? = null,
        val friendJoinsEmail: Boolean? = null,
        val watchingBroadcastEmail: Boolean? = null,
        val communityEmail: Boolean? = null,
        val newFollowerAndroidPush: Boolean? = null,
        val broadcastLiveAndroidPush: Boolean? = null,
        val watchingBroadcastAndroidPush: Boolean? = null,
        val friendJoinsAndroidPush: Boolean? = null
)

enum class NextAccountAction {
    email_verification, mfa_otp_required
}

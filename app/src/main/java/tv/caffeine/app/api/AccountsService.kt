package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.MfaMethod
import tv.caffeine.app.di.IMAGES_BASE_URL

interface AccountsService {
    @POST("v1/account/signin")
    @Headers("No-Authentication: true")
    fun signIn(@Body signInBody: SignInBody): Deferred<Response<SignInResult>>

    @POST("v1/account/forgot-password")
    @Headers("No-Authentication: true")
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

    // Non-body HTTP method like DELETE cannot contain @Body, a workaround for @DELETE("v1/account/{caid}")
    @HTTP(method = "DELETE", path = "v1/account/{caid}", hasBody = true)
    fun deleteAccount(@Path("caid") userId: CAID, @Body deleteAccountBody: DeleteAccountBody): Deferred<Response<Unit>>

    @POST("v1/legal-acceptance")
    fun acceptLegalAgreement(): Deferred<Response<LegalAcceptanceResult>>

    @POST("v1/account/resend-verification")
    fun resendVerification(): Deferred<Response<Void>>

    @POST("v1/account/email/confirm")
    fun confirmEmail(@Body confirmEmailBody: ConfirmEmailBody): Deferred<Response<ConfirmEmailResponse>>

    @GET("v1/account/mfa/email")
    fun sendMFAEmailCode(): Deferred<Response<Void>>

    @POST("v1/account/mfa/")
    fun setMFA(@Body setAccountMfaBody: SendAccountMFABody): Deferred<Response<Void>>
}

class SignInBody(val account: Account, val mfa: MfaCode? = null)
class Account(val username: String? = null, val password: String? = null, val caid: CAID? = null, val loginToken: String? = null)

class MfaCode(val otp: String)

class SignInResult(val accessToken: String, val caid: CAID, val credentials: CaffeineCredentials, val refreshToken: String, val next: NextAccountAction?, val mfaOtpMethod: String?)

class RefreshTokenBody(val refreshToken: String)
class RefreshTokenResult(val credentials: CaffeineCredentials, val next: NextAccountAction)

class CaffeineCredentials(val accessToken: String, val caid: CAID, val credential: String, val refreshToken: String)

class ForgotPasswordBody(val email: String)

class SignUpBody(val account: SignUpAccount, val iid: String?, val tos: Boolean, val recaptchaToken: String?, val arkoseToken: String?)
class SignUpAccount(val username: String, val password: String, val email: String, val dob: String, val countryCode: String)
class SignUpResult(val credentials: CaffeineCredentials, val next: NextAccountAction)

class UpdateAccountBody(val account: AccountUpdateRequest)
class AccountUpdateRequest(val currentPassword: String, val password: String? = null, val email: String? = null)
class AccountUpdateResult(val credentials: CaffeineCredentials, val next: NextAccountAction?)

class UploadAvatarResult(val avatarImagePath: String)
val UploadAvatarResult.avatarImageUrl: String get() = "$IMAGES_BASE_URL$avatarImagePath"

class DeleteAccountBody(val account: DeleteAccountBody.Account) {
    class Account(val currentPassword: String)
}

// omits iOS and web settings
class NotificationSettings(
    val newFollowerEmail: Boolean? = null,
    val weeklySuggestionsEmail: Boolean? = null,
    val broadcastLiveEmail: Boolean? = null,
    val friendJoinsEmail: Boolean? = null,
    val communityEmail: Boolean? = null,
    val broadcastReportEmail: Boolean? = null,
    val newFollowerAndroidPush: Boolean? = null,
    val broadcastLiveAndroidPush: Boolean? = null,
    val friendJoinsAndroidPush: Boolean? = null
) {
    enum class SettingKey {
        new_follower_email,
        weekly_suggestions_email,
        broadcast_live_email,
        friend_joins_email,
        community_email,
        broadcast_report_email,

        new_follower_android_push,
        broadcast_live_android_push,
        friend_joins_android_push
    }

    fun toMap(): MutableMap<SettingKey, Boolean?> {
        return mutableMapOf(
                SettingKey.new_follower_email to newFollowerEmail,
                SettingKey.weekly_suggestions_email to weeklySuggestionsEmail,
                SettingKey.broadcast_live_email to broadcastLiveEmail,
                SettingKey.friend_joins_email to friendJoinsEmail,
                SettingKey.community_email to communityEmail,
                SettingKey.broadcast_report_email to broadcastReportEmail,

                SettingKey.new_follower_android_push to newFollowerAndroidPush,
                SettingKey.broadcast_live_android_push to broadcastLiveAndroidPush,
                SettingKey.friend_joins_android_push to friendJoinsAndroidPush
        )
    }

    companion object {
        fun fromMap(map: Map<SettingKey, Boolean?>): NotificationSettings {
            return NotificationSettings(
                    newFollowerEmail = map[SettingKey.new_follower_email],
                    weeklySuggestionsEmail = map[SettingKey.weekly_suggestions_email],
                    broadcastLiveEmail = map[SettingKey.broadcast_live_email],
                    friendJoinsEmail = map[SettingKey.friend_joins_email],
                    communityEmail = map[SettingKey.community_email],
                    broadcastReportEmail = map[SettingKey.broadcast_report_email],

                    newFollowerAndroidPush = map[SettingKey.new_follower_android_push],
                    broadcastLiveAndroidPush = map[SettingKey.broadcast_live_android_push],
                    friendJoinsAndroidPush = map[SettingKey.friend_joins_android_push]
            )
        }
    }
}

class LegalAcceptanceResult(val success: Boolean)

class ConfirmEmailBody(val code: String, val caid: CAID)

class ConfirmEmailResponse(val emailConfirmed: Boolean)

class SendAccountMFABody(val mfa: SendAccountMFABody.Mfa) {
    class Mfa(val method: MfaMethod, val otp: String)
}

enum class NextAccountAction {
    email_verification, mfa_otp_required, legal_acceptance_required
}

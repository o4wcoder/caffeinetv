package tv.caffeine.app.api

import tv.caffeine.app.stage.OUT_OF_CAPACITY_REYES_V4

data class ApiErrorResult(val errors: ApiError?, val uuid: String? = null, val type: String? = null, val reason: String? = null)

fun ApiErrorResult.isTokenExpirationError() = errors?._token?.isNullOrEmpty() == false
fun ApiErrorResult.isVersionCheckError() = errors?._expired?.contains("version") == true
fun VersionCheckError() = ApiErrorResult(ApiError(_expired = listOf("version")))
fun ApiErrorResult.isIdentityRateLimitExceeded() = errors?._identity?.contains("Rate Limit Exceeded.") == true
fun ApiErrorResult.isMustVerifyEmailError() = errors?._unverifiedEmail?.isNullOrEmpty() == false
fun ApiErrorResult.isRecordNotFoundError() = errors?._record?.contains("could not be found") == true
fun ApiErrorResult.isVerificationFailedError() = errors?.code?.contains("Verification failed.") == true
fun ApiErrorResult.isOutOfCapacityError() = type == OUT_OF_CAPACITY_REYES_V4
fun ApiErrorResult.isEmailError() = errors?.email?.isNullOrEmpty() == false
fun ApiErrorResult.isUsernameError() = errors?.username?.isNullOrEmpty() == false
fun ApiErrorResult.isPasswordError() = errors?.password?.isNullOrEmpty() == false
fun ApiErrorResult.isBirtdateError() = errors?.dob?.isNullOrEmpty() == false
fun ApiErrorResult.isPasswordResetCodeError() = errors?.code?.isNullOrEmpty() == false
fun RefreshTokenMissingError() = ApiErrorResult(ApiError(_token = listOf("Refresh token missing")))

data class ApiError(
    val _error: List<String>? = null,
    val _denied: List<String>? = null,
    val username: List<String>? = null,
    val password: List<String>? = null,
    val dob: List<String>? = null,
    val currentPassword: List<String>? = null,
    val email: List<String>? = null,
    val otp: List<String>? = null,
    val code: List<String>? = null,
    val _token: List<String>? = null,
    val _record: List<String>? = null,
    val _expired: List<String>? = null,
    val _identity: List<String>? = null,
    val _unverifiedEmail: List<String>? = null
)

val ApiErrorResult.generalErrorsString get() = errors?._error?.joinToString("\n")
val ApiErrorResult.deniedErrorsString get() = errors?._denied?.joinToString("\n")
val ApiErrorResult.usernameErrorsString get() = errors?.username?.joinToString("\n")
val ApiErrorResult.passwordErrorsString get() = errors?.password?.joinToString("\n")
val ApiErrorResult.dobErrorsString get() = errors?.dob?.joinToString("\n")
val ApiErrorResult.currentPasswordErrorsString get() = errors?.currentPassword?.joinToString("\n")
val ApiErrorResult.emailErrorsString get() = errors?.email?.joinToString("\n")
val ApiErrorResult.otpErrorsString get() = errors?.otp?.joinToString("\n")
val ApiErrorResult.resetPasswordErrorString get() = errors?.code?.joinToString("\n")

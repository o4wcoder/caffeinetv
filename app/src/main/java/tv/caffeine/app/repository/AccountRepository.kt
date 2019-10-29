package tv.caffeine.app.repository

import android.content.res.Resources
import com.google.gson.Gson
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountUpdateRequest
import tv.caffeine.app.api.AccountUpdateResult
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ApiError
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.SignUpBody
import tv.caffeine.app.api.UpdateAccountBody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountsService: AccountsService,
    private val tokenStore: TokenStore,
    private val followManager: FollowManager,
    private val resources: Resources,
    private val gson: Gson
) {
    suspend fun resendVerification(): CaffeineEmptyResult {
        return accountsService.resendVerification().awaitEmptyAndParseErrors(gson)
    }

    suspend fun signUp(signUpBody: SignUpBody) =
        accountsService.signUp(signUpBody).awaitAndParseErrors(gson)

    suspend fun updateEmail(currentPassword: String, newEmail: String): CaffeineResult<AccountUpdateResult> {
        val update = UpdateAccountBody(AccountUpdateRequest(currentPassword, email = newEmail))
        val result = accountsService.updateAccount(update).awaitAndParseErrors(gson)
        if (result is CaffeineResult.Success) {
            tokenStore.storeCredentials(result.value.credentials)
            followManager.loadMyUserDetails() // update the user so emailVerified is false
        }
        return result
    }

    suspend fun updatePassword(currentPassword: String, password1: String, password2: String): CaffeineResult<AccountUpdateResult> {
        if (password1 != password2) return CaffeineResult.Error(
            ApiErrorResult(
                ApiError(password = listOf(resources.getString(
                    R.string.error_passwords_dont_match)))
            )
        )
        val update = UpdateAccountBody(AccountUpdateRequest(currentPassword, password = password1))
        val result = accountsService.updateAccount(update).awaitAndParseErrors(gson)
        if (result is CaffeineResult.Success) {
            tokenStore.storeCredentials(result.value.credentials)
        }
        return result
    }
}
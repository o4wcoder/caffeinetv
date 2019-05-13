package tv.caffeine.app.profile

import android.content.res.Resources
import com.google.gson.Gson
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountUpdateRequest
import tv.caffeine.app.api.AccountUpdateResult
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ApiError
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.UpdateAccountBody
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

class UpdatePasswordUseCase @Inject constructor(
    private val accountsService: AccountsService,
    private val tokenStore: TokenStore,
    private val resources: Resources,
    private val gson: Gson
) {
    suspend operator fun invoke(currentPassword: String, password1: String, password2: String): CaffeineResult<AccountUpdateResult> {
        if (password1 != password2) return CaffeineResult.Error(ApiErrorResult(ApiError(password = listOf(resources.getString(R.string.error_passwords_dont_match)))))
        val update = UpdateAccountBody(AccountUpdateRequest(currentPassword, password = password1))
        val result = accountsService.updateAccount(update).awaitAndParseErrors(gson)
        if (result is CaffeineResult.Success) {
            tokenStore.storeCredentials(result.value.credentials)
        }
        return result
    }
}

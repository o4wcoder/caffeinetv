package tv.caffeine.app.profile

import android.content.res.Resources
import com.google.gson.Gson
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.R
import javax.inject.Inject

class UpdatePasswordUseCase @Inject constructor(
        private val accountsService: AccountsService,
        private val tokenStore: TokenStore,
        private val resources: Resources,
        private val gson: Gson
) {
    suspend operator fun invoke(currentPassword: String, password1: String, password2 : String): CaffeineResult<AccountUpdateResult> {
        if (password1 != password2) return CaffeineResult.Error(ApiErrorResult(ApiError(password = arrayOf(resources.getString(R.string.error_passwords_dont_match)))))
        val update = UpdateAccountBody(AccountUpdateRequest(currentPassword, password = password1))
        val result = accountsService.updateAccount(update).awaitAndParseErrors(gson)
        if (result is CaffeineResult.Success) {
            tokenStore.storeCredentials(result.value.credentials)
        }
        return result
    }
}

package tv.caffeine.app.profile

import com.google.gson.Gson
import tv.caffeine.app.api.AccountUpdateRequest
import tv.caffeine.app.api.AccountUpdateResult
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.UpdateAccountBody
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

class UpdateEmailUseCase @Inject constructor(
    private val accountsService: AccountsService,
    private val tokenStore: TokenStore,
    private val gson: Gson
) {
    suspend operator fun invoke(currentPassword: String, email: String): CaffeineResult<AccountUpdateResult> {
        val update = UpdateAccountBody(AccountUpdateRequest(currentPassword, email = email))
        val result = accountsService.updateAccount(update).awaitAndParseErrors(gson)
        if (result is CaffeineResult.Success) {
            tokenStore.storeCredentials(result.value.credentials)
        }
        return result
    }
}

package tv.caffeine.app.repository

import com.google.gson.Gson
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.SignUpBody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson
) {
    suspend fun resendVerification(): CaffeineEmptyResult {
        return accountsService.resendVerification().awaitEmptyAndParseErrors(gson)
    }

    suspend fun signUp(signUpBody: SignUpBody) =
        accountsService.signUp(signUpBody).awaitAndParseErrors(gson)
}
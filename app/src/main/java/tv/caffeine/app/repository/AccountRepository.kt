package tv.caffeine.app.repository

import com.google.gson.Gson
import timber.log.Timber
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
    suspend fun resendVerification() {
        val result = accountsService.resendVerification().awaitEmptyAndParseErrors(gson)
        when (result) {
            is CaffeineEmptyResult.Success -> Timber.d("Successfully resent verification email")
            is CaffeineEmptyResult.Error -> Timber.e("Error resending verification email")
            is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
        }
    }

    suspend fun signUp(signUpBody: SignUpBody) =
        accountsService.signUp(signUpBody).awaitAndParseErrors(gson)
}
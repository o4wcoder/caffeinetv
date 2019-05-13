package tv.caffeine.app.login

import com.google.gson.Gson
import tv.caffeine.app.api.Account
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.NextAccountAction
import tv.caffeine.app.api.SignInBody
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val gson: Gson,
    private val accountsService: AccountsService,
    private val tokenStore: TokenStore,
    private val authWatcher: AuthWatcher
) {

    suspend operator fun invoke(username: String, password: String): CaffeineResult<SignInResult> {
        val signInBody = SignInBody(Account(username, password))
        val result = accountsService.signIn(signInBody).awaitAndParseErrors(gson)
        postLogin(result)
        return result
    }

    private fun postLogin(result: CaffeineResult<SignInResult>) {
        if (result !is CaffeineResult.Success) return
        val signInResult = result.value
        if (signInResult.next == null || signInResult.next == NextAccountAction.legal_acceptance_required || signInResult.next == NextAccountAction.email_verification) {
            tokenStore.storeSignInResult(signInResult)
            authWatcher.onSignIn()
        }
    }
}

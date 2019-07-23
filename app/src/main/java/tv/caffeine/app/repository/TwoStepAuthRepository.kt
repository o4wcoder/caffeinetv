package tv.caffeine.app.repository

import com.google.gson.Gson
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.SendAccountMFABody
import tv.caffeine.app.api.model.MfaMethod
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TwoStepAuthRepository @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson
) {

    suspend fun sendVerificationCode(code: String) =
        accountsService.setMFA(SendAccountMFABody(SendAccountMFABody.Mfa(MfaMethod.EMAIL, code)))
            .awaitEmptyAndParseErrors(gson)

    suspend fun sendMTAEmailCode() =
        accountsService.sendMFAEmailCode().awaitEmptyAndParseErrors(gson)

    suspend fun disableAuth() =
        accountsService.setMFA(SendAccountMFABody(SendAccountMFABody.Mfa(MfaMethod.NONE, "")))
            .awaitEmptyAndParseErrors(gson)
}
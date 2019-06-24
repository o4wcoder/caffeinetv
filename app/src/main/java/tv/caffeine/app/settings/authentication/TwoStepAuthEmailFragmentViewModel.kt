package tv.caffeine.app.settings.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.SendAccountMFABody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.api.model.MfaMethod
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import javax.inject.Inject

class TwoStepAuthEmailFragmentViewModel @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson
) : ViewModel() {

    private val update = MutableLiveData<Event<CaffeineEmptyResult>>()

    fun sendVerificationCode(code: String): LiveData<Event<CaffeineEmptyResult>> {
        viewModelScope.launch {
            val result = accountsService.setMFA(SendAccountMFABody(SendAccountMFABody.Mfa(MfaMethod.EMAIL, code)))
                .awaitEmptyAndParseErrors(gson)
            update.value = Event(result)
        }
        return update
    }

    fun sendMTAEmailCode() {
        viewModelScope.launch {
            val result = accountsService.sendMFAEmailCode().awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> Timber.d("Successful send MFA email code")
                is CaffeineEmptyResult.Error -> Timber.d("Error attempting to send MFA email code ${result.error}")
                is CaffeineEmptyResult.Failure -> Timber.d("Failure attempting to send MFA email code ${result.throwable}")
            }
        }
    }
}
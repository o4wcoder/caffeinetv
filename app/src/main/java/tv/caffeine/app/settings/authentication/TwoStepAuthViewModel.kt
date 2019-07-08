package tv.caffeine.app.settings.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
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

class TwoStepAuthViewModel @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson
) : ViewModel() {
    private val update = MutableLiveData<Event<CaffeineEmptyResult>>()
    private val _mfaEnabled = MutableLiveData<Event<Boolean>>()
    val mfaEnabled: LiveData<Event<Boolean>> = _mfaEnabled.map { it }
    private val _startEnableMfa = MutableLiveData<Event<Boolean>>()
    val startEnableMfa: LiveData<Event<Boolean>> = _startEnableMfa.map { it }

    fun startEnableMtaSetup() {
        sendMTAEmailCode()
        _startEnableMfa.value = Event(true)
    }

    fun updateMfaEnabled(enabled: Boolean) {
        _mfaEnabled.value = Event(enabled)
    }

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

    fun disableAuth() {
        viewModelScope.launch {
            val result = accountsService.setMFA(SendAccountMFABody(SendAccountMFABody.Mfa(MfaMethod.NONE, "")))
                .awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> {
                    updateMfaEnabled(false)
                    Timber.d("Success disabling Two-Step Authentication")
                }
                is CaffeineEmptyResult.Error -> Timber.e("Error attempting to disable Two-Step Authentication, ${result.error}")
                is CaffeineEmptyResult.Failure -> Timber.e(
                    result.throwable,
                    "Failed to to disable Two-Step Authentication"
                )
            }
        }
    }
}
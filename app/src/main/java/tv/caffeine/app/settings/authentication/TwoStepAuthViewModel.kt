package tv.caffeine.app.settings.authentication

import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.BR
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.repository.TwoStepAuthRepository
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

private const val MIN_CODE_LENGTH = 1

class TwoStepAuthViewModel @Inject constructor(
    private val twoStepAuthRepository: TwoStepAuthRepository
) : CaffeineViewModel() {
    private var verificationCode = ""

    private val _sendVerificationCodeUpdate = MutableLiveData<Event<CaffeineEmptyResult>>()
    val sendVerificationCodeUpdate = _sendVerificationCodeUpdate.map { it }
    private val _mfaEnabledUpdate = MutableLiveData<Event<Boolean>>()
    val mfaEnabledUpdate: LiveData<Event<Boolean>> = _mfaEnabledUpdate.map { it }
    private val _startEnableMfaUpdate = MutableLiveData<Event<Boolean>>()
    val startEnableMfaUpdate: LiveData<Event<Boolean>> = _startEnableMfaUpdate.map { it }

    fun startEnableMtaSetup() {
        sendMTAEmailCode()
        _startEnableMfaUpdate.value = Event(true)
    }

    fun updateMfaEnabled(enabled: Boolean) {
        _mfaEnabledUpdate.value = Event(enabled)
    }

    fun sendVerificationCode(code: String): LiveData<Event<CaffeineEmptyResult>> {
        viewModelScope.launch {
            val result = twoStepAuthRepository.sendVerificationCode(code)
            _sendVerificationCodeUpdate.value = Event(result)
        }
        return _sendVerificationCodeUpdate
    }

    fun sendMTAEmailCode() {
        viewModelScope.launch {
            val result = twoStepAuthRepository.sendMTAEmailCode()
            when (result) {
                is CaffeineEmptyResult.Success -> Timber.d("Successful send MFA email code")
                is CaffeineEmptyResult.Error -> Timber.d("Error attempting to send MFA email code ${result.error}")
                is CaffeineEmptyResult.Failure -> Timber.d("Failure attempting to send MFA email code ${result.throwable}")
            }
        }
    }

    fun disableAuth() {
        viewModelScope.launch {
            val result = twoStepAuthRepository.disableAuth()
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

    fun onVerificationCodeTextChanged(text: CharSequence?) {
        verificationCode = text.toString()
        notifyPropertyChanged(BR.verificationCodeButtonEnabled)
    }

    @Bindable
    fun isVerificationCodeButtonEnabled() = verificationCode.length >= MIN_CODE_LENGTH

    fun onVerificationCodeButtonClick() = sendVerificationCode(verificationCode)
}
package tv.caffeine.app.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.NextAccountAction
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.passwordErrorsString
import tv.caffeine.app.api.usernameErrorsString
import javax.inject.Inject

sealed class SignInOutcome {
    object Success : SignInOutcome()
    object MFARequired : SignInOutcome()
    object MustAcceptTerms : SignInOutcome()
    class Error(val formError: String?, val usernameError: String?, val passwordError: String?) : SignInOutcome()
    class Failure(val exception: Throwable) : SignInOutcome()
}

class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase
) : ViewModel() {

    private val _signInOutcome = MutableLiveData<SignInOutcome>()
    val signInOutcome: LiveData<SignInOutcome> = _signInOutcome.map { it }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = signInUseCase(username, password)
            _signInOutcome.value = when (result) {
                is CaffeineResult.Success -> processSuccess(result.value)
                is CaffeineResult.Error -> processError(result.error)
                is CaffeineResult.Failure -> processFailure(result.throwable)
            }
        }
    }

    private fun processSuccess(signInResult: SignInResult) =
            when (signInResult.next) {
                NextAccountAction.mfa_otp_required -> SignInOutcome.MFARequired
                NextAccountAction.legal_acceptance_required -> SignInOutcome.MustAcceptTerms
                else -> SignInOutcome.Success
            }

    private fun processError(error: ApiErrorResult): SignInOutcome {
        val formError = error.generalErrorsString
        val usernameError = error.usernameErrorsString
        val passwordError = error.passwordErrorsString
        return SignInOutcome.Error(formError, usernameError, passwordError)
    }

    private fun processFailure(exception: Throwable) = SignInOutcome.Failure(exception)
}

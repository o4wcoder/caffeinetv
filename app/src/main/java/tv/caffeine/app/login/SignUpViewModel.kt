package tv.caffeine.app.login

import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.SignUpAccount
import tv.caffeine.app.api.SignUpBody
import tv.caffeine.app.api.SignUpResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class SignUpViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : CaffeineViewModel() {

    private val _signUpUpdate = MutableLiveData<Event<CaffeineResult<SignUpResult>>>()

    var email = ""
        set(value) {
            field = value
            notifyChange()
        }
    var username = ""
        set(value) {
            field = value
            notifyChange()
        }
    var password = ""
        set(value) {
            field = value
            notifyChange()
        }
    var birthdate = ""
        set(value) {
            field = value
            notifyChange()
        }

    var passwordIsValid = false
        set(value) {
            field = value
            notifyChange()
        }

    @Bindable
    fun isSignUpButtonEnabled() = email.isNotEmpty() && username.isNotEmpty() && passwordIsValid && birthdate.isNotEmpty()

    fun signIn(dob: String, recaptchaToken: String?, arkoseToken: String?, iid: String?): LiveData<Event<CaffeineResult<SignUpResult>>> {
        viewModelScope.launch {
            val countryCode = "US"
            val account = SignUpAccount(username, password, email, dob, countryCode)
            val signUpBody = SignUpBody(account, iid, true, recaptchaToken, arkoseToken)
            val result = accountRepository.signUp(signUpBody)
            _signUpUpdate.value = Event(result)
        }
        return _signUpUpdate
    }
}
package tv.caffeine.app.login

import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class ResetPasswordViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : CaffeineViewModel() {

    private var isPasswordValid = true
    private val _resetPasswordUpdate = MutableLiveData<Event<CaffeineEmptyResult>>()

    var password = ""
        set(value) {
            field = value
            notifyChange()
        }

    var confirmPassword = ""
        set(value) {
            field = value
            notifyChange()
        }

    @Bindable
    fun isResetPasswordButtonEnabled() = password.isNotEmpty() && confirmPassword.isNotEmpty()

    private fun clearErrors() {
        isPasswordValid = true
        notifyChange()
    }

    fun validatePasswords(): Boolean {
        isPasswordValid = password == confirmPassword
        notifyChange()
        return isPasswordValid
    }

    fun resetPassword(code: String): LiveData<Event<CaffeineEmptyResult>> {
        clearErrors()
        viewModelScope.launch {
            val result = accountRepository.resetPassword(code, password)
            _resetPasswordUpdate.value = Event(result)
        }
        return _resetPasswordUpdate
    }
}
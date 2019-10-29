package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.AccountUpdateResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.repository.AccountRepository
import javax.inject.Inject

class UpdateProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val update = MutableLiveData<CaffeineResult<AccountUpdateResult>>()

    fun updateEmail(currentPassword: String, email: String): LiveData<CaffeineResult<AccountUpdateResult>> {
        viewModelScope.launch {
            val result = accountRepository.updateEmail(currentPassword, email)
            update.value = result
        }
        return update
    }

    fun updatePassword(currentPassword: String, password1: String, password2: String): LiveData<CaffeineResult<AccountUpdateResult>> {
        viewModelScope.launch {
            val result = accountRepository.updatePassword(currentPassword, password1, password2)
            update.value = result
        }
        return update
    }
}

package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import tv.caffeine.app.api.AccountUpdateResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel

class UpdateProfileViewModel(
        private val updateEmailUseCase: UpdateEmailUseCase,
        private val updatePasswordUseCase: UpdatePasswordUseCase
) : CaffeineViewModel() {

    private val update = MutableLiveData<CaffeineResult<AccountUpdateResult>>()

    fun updateEmail(currentPassword: String, email: String): LiveData<CaffeineResult<AccountUpdateResult>> {
        launch {
            val result = updateEmailUseCase(currentPassword, email)
            update.value = result
        }
        return update
    }

    fun updatePassword(currentPassword: String, password1: String, password2: String): LiveData<CaffeineResult<AccountUpdateResult>> {
        launch {
            val result = updatePasswordUseCase(currentPassword, password1, password2)
            update.value = result
        }
        return update
    }
}

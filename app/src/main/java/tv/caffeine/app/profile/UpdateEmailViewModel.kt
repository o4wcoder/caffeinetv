package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import tv.caffeine.app.api.AccountUpdateResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel

class UpdateEmailViewModel(
        private val updateEmailUseCase: UpdateEmailUseCase
) : CaffeineViewModel() {

    val update: LiveData<CaffeineResult<AccountUpdateResult>> get() = _update
    private val _update = MutableLiveData<CaffeineResult<AccountUpdateResult>>()

    fun updateEmail(currentPassword: String, email: String): LiveData<CaffeineResult<AccountUpdateResult>> {
        launch {
            val result = updateEmailUseCase(currentPassword, email)
            _update.value = result
        }
        return update
    }
}

package tv.caffeine.app.login

import android.view.View
import androidx.databinding.Bindable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class WelcomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : CaffeineViewModel() {

    private var hasEmailBeenResent = false

    @Bindable
    var email = ""
        set(value) {
            field = value
            notifyChange()
        }

    @Bindable
    fun getEmailDisplayVisibility() = if (hasEmailBeenResent) View.VISIBLE else View.INVISIBLE

    @Bindable
    fun getResendEmailVisibility() = if (hasEmailBeenResent) View.INVISIBLE else View.VISIBLE

    fun onResendEmailClick() {
        hasEmailBeenResent = true
        notifyChange()
        sendVerificationEmail()
    }

    private fun sendVerificationEmail() {
        viewModelScope.launch {
            accountRepository.resendVerification()
        }
    }
}
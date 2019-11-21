package tv.caffeine.app.login

import android.view.View
import android.widget.TextView
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.ConfirmEmailBody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class ConfirmEmailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val followManager: FollowManager
) : CaffeineViewModel() {

    private var isLoading = true
    private var isError = false
    private var hasResentEmail = false
    private var hasResendFailed = false

    var isSuccess = false

    @Bindable
    fun getLoadingVisibility() = if (isLoading) View.VISIBLE else View.GONE

    @Bindable
    fun getTitleVisiblity() = if (isSuccess) View.GONE else View.VISIBLE

    @Bindable
    fun getSubtitleVisibility() = if (isError) View.VISIBLE else View.GONE

    @Bindable
    fun getSubtitleText() = if (hasResendFailed) R.string.could_not_send_email else R.string.we_couldnt_confirm_your_account

    @Bindable
    fun getEmailConfirmationVisibility() = if (hasResentEmail) View.VISIBLE else View.GONE

    @Bindable
    fun getSuccessVisibility() = if (isSuccess) View.VISIBLE else View.GONE

    @Bindable
    fun getButtonVisibility() = if (isLoading) View.GONE else View.VISIBLE

    @Bindable
    fun getTitleText() = if (isError) R.string.somethings_not_right else R.string.confirming_your_account

    @Bindable
    fun getButtonText() = if (isSuccess) R.string.lets_go else R.string.resend_email

    @Bindable
    var isButtonEnabled: Boolean = true
        set(value) {
            field = value
            notifyChange()
        }

    @Bindable
    var userEmailAddress: String? = null
        set(value) {
            field = value
            notifyChange()
        }

    fun load(code: String, caid: String) {
        viewModelScope.launch {
            val body = ConfirmEmailBody(code, caid)
            val result = accountRepository.confirmEmail(body)

            when (result) {
                is CaffeineResult.Success -> handleSuccess()
                is CaffeineResult.Error -> {
                    handleError()
                    Timber.e("Error resending verification email")
                }
                is CaffeineResult.Failure -> {
                    handleError()
                    Timber.e(result.throwable)
                }
            }
        }
    }

    private fun handleSuccess() {
        isLoading = false
        isSuccess = true
        notifyChange()
    }

    private fun handleError() {
        isLoading = false
        isError = true
        getUserEmail()
        notifyChange()
    }

    private fun getUserEmail() {
        viewModelScope.launch {
            val user = followManager.loadMyUserDetails()
            userEmailAddress = user?.email
        }
    }

    fun resendEmail() {
        viewModelScope.launch {
            val result = accountRepository.resendVerification()
            when (result) {
                is CaffeineEmptyResult.Success -> handleResendEmailSuccess()
                is CaffeineEmptyResult.Error -> {
                    handleResendEmailError()
                    Timber.e("Error resending verification email")
                }
                is CaffeineEmptyResult.Failure -> {
                    handleResendEmailError()
                    Timber.e(result.throwable)
                }
            }
        }
    }

    private fun handleResendEmailSuccess() {
        isButtonEnabled = false
        hasResentEmail = true
    }

    private fun handleResendEmailError() {
        isButtonEnabled = false
        hasResendFailed = true
    }
}

@BindingAdapter("emailAddress")
fun TextView.setEmailConfirmationText(userEmail: String?) {
    this.text = this.context.getString(R.string.sending_verification_email_message, userEmail)
}
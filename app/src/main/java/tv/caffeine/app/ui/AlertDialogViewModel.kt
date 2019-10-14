package tv.caffeine.app.ui

import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.maybeShow
import javax.inject.Inject

private const val REQUEST_SHOW_EMAIL_VERIFICATION_DIALOG = 111

class AlertDialogViewModel @Inject constructor(
    private val followManager: FollowManager,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private var user: User? = null
    private var fragmentManager: FragmentManager? = null

    init {
        viewModelScope.launch {
            followManager.loadMyUserDetails()?.let { user ->
                this@AlertDialogViewModel.user = user
            }
        }
    }

    fun isUserVerified(): Boolean {
        return user?.let {
            it.emailVerified
        } ?: false
    }

    fun showVerifyEmailDialog(fragment: Fragment, fragmentManager: FragmentManager?) {
        this.fragmentManager = fragmentManager
        AlertDialogFragment.verifyEmail().also {
            it.setTargetFragment(fragment, REQUEST_SHOW_EMAIL_VERIFICATION_DIALOG)
            it.maybeShow(fragmentManager, "verifyEmail")
        }
    }

    fun resendEmail() {
        viewModelScope.launch {
            val result = accountRepository.resendVerification()
            when (result) {
                is CaffeineEmptyResult.Success -> showResendEmailSuccess()
                is CaffeineEmptyResult.Error -> Timber.e("Error resending verification email")
                is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    fun observeFollowEvents(lifecycleOwner: LifecycleOwner, fragment: Fragment) {
        followManager.followResult.observe(lifecycleOwner, Observer { result ->
            result.getContentIfNotHandled()?.let {
                when (it) {
                    is CaffeineEmptyResult.Error -> {
                        if (it.error.isMustVerifyEmailError()) {
                            showVerifyEmailDialog(fragment, fragment.fragmentManager)
                        } else {
                            Timber.e("Couldn't follow user ${it.error}")
                        }
                    }
                    is CaffeineEmptyResult.Failure -> Timber.e(it.throwable)
                }
            }
        })
    }

    @UiThread
    private fun showResendEmailSuccess() {
        user?.email?.let { emailAddress ->
            AlertDialogFragment.withEmailForSuccess(emailAddress).also {
                it.maybeShow(fragmentManager, "emailSent")
            }
        }
    }
}
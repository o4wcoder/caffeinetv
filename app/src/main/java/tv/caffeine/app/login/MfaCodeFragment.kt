package tv.caffeine.app.login

import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.MfaCode
import tv.caffeine.app.api.NextAccountAction
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.otpErrorsString
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentMfaCodeBinding
import tv.caffeine.app.settings.authentication.TwoStepAuthViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class MfaCodeFragment @Inject constructor(
    private val tokenStore: TokenStore,
    private val authWatcher: AuthWatcher,
    private val firebaseAnalytics: FirebaseAnalytics
) : CaffeineFragment(R.layout.fragment_mfa_code) {

    private lateinit var binding: FragmentMfaCodeBinding
    private val args by navArgs<MfaCodeFragmentArgs>()
    private val viewModel: TwoStepAuthViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentMfaCodeBinding.bind(view)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.submitMfaCodeButton.setOnClickListener { submitMfaCode(false) }
        binding.mfaCodeEditText.setOnActionGo { submitMfaCode(false) }
        binding.mfaCodeEditText.afterTextChanged { viewModel.setVerificationCode(it) }
        binding.mfaCodeResendEmailText.setOnClickListener { submitMfaCode(true) }
    }

    private fun clearErrors() {
        binding.mfaCodeEditText.clearError()
    }

    private fun submitMfaCode(skipMfaCode: Boolean) {
        clearErrors()
        val username = args.username
        val password = args.password
        val caid = args.caid
        val loginToken = args.loginToken
        val code = if (skipMfaCode) null else MfaCode(binding.mfaCodeEditText.text)
        viewModel.signInWithMfaCode(username, password, caid, loginToken, code)
            .observe(viewLifecycleOwner, Observer { event ->
                event.getContentIfNotHandled()?.let { result ->
                    when (result) {
                        is CaffeineResult.Success -> onSuccess(result.value)
                        is CaffeineResult.Error -> onError(result.error)
                        is CaffeineResult.Failure -> onFailure(result.throwable)
                    }
                }
            })
    }

    @UiThread
    private fun onSuccess(result: SignInResult) {
        if (result.next != NextAccountAction.mfa_otp_required) {
            firebaseAnalytics.logEvent(FirebaseEvent.MFASignInSuccess)
            tokenStore.storeSignInResult(result)
            val navController = findNavController()
            navController.popBackStack(R.id.landingFragment, true)
            navController.safeNavigate(R.id.lobbySwipeFragment)
            authWatcher.onSignIn()
        }
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        Timber.d("Error: $error")
        val errorMessages = listOfNotNull(error.generalErrorsString, error.otpErrorsString)
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            binding.mfaCodeEditText.showError(it)
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "sign in failure")
        showSnackbar(R.string.sign_in_failure)
    }
}

package tv.caffeine.app.login

import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.databinding.FragmentSignInBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class SignInFragment @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : CaffeineFragment(R.layout.fragment_sign_in) {

    @VisibleForTesting
    lateinit var binding: FragmentSignInBinding

    private val signInViewModel: SignInViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentSignInBinding.bind(view)
        configure(binding)
    }

    private fun configure(binding: FragmentSignInBinding) {
        Navigation.createNavigateOnClickListener(R.id.forgotFragment).let {
            binding.resetPasswordTextView.setOnClickListener(it)
        }
        binding.signInButton.setOnClickListener { login() }
        binding.usernameEditTextLayout.afterTextChanged { validate() }
        binding.passwordEditTextLayout.setOnActionGo { login() }
        binding.passwordEditTextLayout.afterTextChanged { validate() }
        signInViewModel.signInOutcome.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { outcome ->
                when (outcome) {
                    is SignInOutcome.Success -> onSuccess()
                    is SignInOutcome.MFARequired -> onMfaRequired()
                    is SignInOutcome.MustAcceptTerms -> onMustAcceptTerms()
                    is SignInOutcome.Error -> onError(outcome)
                    is SignInOutcome.Failure -> onFailure(outcome.exception)
                }
            }
        })
    }

    private fun clearErrors() {
        binding.formErrorTextView.isInvisible = true
        binding.usernameEditTextLayout.clearError()
    }

    private fun login() {
        clearErrors()
        val username = binding.usernameEditTextLayout.text
        val password = binding.passwordEditTextLayout.text
        signInViewModel.login(username, password)
    }

    private fun validate() {
        binding.signInButton.isEnabled =
            !binding.usernameEditTextLayout.isEmpty() && !binding.passwordEditTextLayout.isEmpty()
    }

    @UiThread
    private fun onSuccess() {
        firebaseAnalytics.logEvent(FirebaseEvent.SignInSuccess)
        (activity as MainActivity).reloadMyProfileViewModelOnSign()
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.safeNavigate(R.id.lobbySwipeFragment)
    }

    @UiThread
    private fun onMfaRequired() {
        firebaseAnalytics.logEvent(FirebaseEvent.SignInContinueToMFA)
        val navController = findNavController()
        val username = binding.usernameEditTextLayout.text
        val password = binding.passwordEditTextLayout.text
        val action =
            SignInFragmentDirections.actionSignInFragmentToMfaCodeFragment(username, password, null, null)
        navController.safeNavigate(action)
    }

    @UiThread
    private fun onMustAcceptTerms() {
        firebaseAnalytics.logEvent(FirebaseEvent.SignInContinueToTerms)
        val action = SignInFragmentDirections.actionSignInFragmentToLegalAgreementFragment()
        findNavController().safeNavigate(action)
    }

    @UiThread
    private fun onError(error: SignInOutcome.Error) {
        Timber.d("Error: $error")
        val errorMessages = listOfNotNull(
                error.formError,
                error.usernameError,
                error.passwordError
        )
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            binding.formErrorTextView.text = it
            binding.formErrorTextView.isInvisible = false
            binding.usernameEditTextLayout.showError(null)
            binding.passwordEditTextLayout.showError(null)
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "sign in failure")
        showSnackbar(R.string.sign_in_failure)
    }
}

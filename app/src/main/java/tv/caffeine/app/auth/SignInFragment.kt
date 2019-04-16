package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.databinding.FragmentSignInBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnActionGo
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

class SignInFragment : CaffeineFragment() {

    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: FragmentSignInBinding

    private val signInViewModel: SignInViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return FragmentSignInBinding.inflate(inflater, container, false).run {
            configure(this)
            binding = this
            root
        }
    }

    private fun configure(binding: FragmentSignInBinding) {
        View.OnClickListener {
            Navigation.createNavigateOnClickListener(R.id.forgotFragment)
        }.let {
            binding.resetPasswordTextView.setOnClickListener(it)
            binding.resetPasswordPromptTextView.setOnClickListener(it)
        }
        binding.signInButton.setOnClickListener { login() }
        binding.passwordEditText.setOnActionGo { login() }
        signInViewModel.signInOutcome.observe(viewLifecycleOwner, Observer { outcome ->
            when(outcome) {
                is SignInOutcome.Success -> onSuccess()
                is SignInOutcome.MFARequired -> onMfaRequired()
                is SignInOutcome.MustAcceptTerms -> onMustAcceptTerms()
                is SignInOutcome.Error -> onError(outcome)
                is SignInOutcome.Failure -> onFailure(outcome.exception)
            }
        })
    }

    private fun clearErrors() {
        binding.formErrorTextView.text = getString(R.string.sign_in_description)
    }

    private fun login() {
        clearErrors()
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        signInViewModel.login(username, password)
    }

    @UiThread
    private fun onSuccess() {
        firebaseAnalytics.logEvent(FirebaseEvent.SignInSuccess)
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.safeNavigate(R.id.lobbySwipeFragment)
    }

    @UiThread
    private fun onMfaRequired() {
        firebaseAnalytics.logEvent(FirebaseEvent.SignInContinueToMFA)
        val navController = findNavController()
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
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
        binding.formErrorTextView.text = when {
            !error.formError.isNullOrEmpty() -> error.formError
            !error.usernameError.isNullOrEmpty() -> error.usernameError
            !error.passwordError.isNullOrEmpty() -> error.passwordError
            else -> getString(R.string.sign_in_description)
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "Error while trying to sign in") // TODO show error message
        binding.formErrorTextView.setText(R.string.unknown_error)
    }
}

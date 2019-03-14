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
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentSignInBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnActionGo
import tv.caffeine.app.util.safeNavigate

class SignInFragment : CaffeineFragment() {

    private lateinit var binding: FragmentSignInBinding

    private val signInViewModel: SignInViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.forgotButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.forgotFragment))
        binding.signInButton.setOnClickListener { login() }
        binding.passwordEditText.setOnActionGo { login() }
    }

    private fun clearErrors() {
        binding.formErrorTextView.text = null
        binding.usernameTextInputLayout.error = null
        binding.passwordTextInputLayout.error = null
    }

    private fun login() {
        clearErrors()
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        signInViewModel.login(username, password).observe(viewLifecycleOwner, Observer { outcome ->
            when(outcome) {
                is SignInOutcome.Success -> onSuccess()
                is SignInOutcome.MFARequired -> onMfaRequired()
                is SignInOutcome.MustAcceptTerms -> onMustAcceptTerms()
                is SignInOutcome.Error -> onError(outcome)
                is SignInOutcome.Failure -> onFailure(outcome.exception)
            }
        })
    }

    @UiThread
    private fun onSuccess() {
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.safeNavigate(R.id.lobbySwipeFragment)
    }

    @UiThread
    private fun onMfaRequired() {
        val navController = findNavController()
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val action =
                SignInFragmentDirections.actionSignInFragmentToMfaCodeFragment(username, password, null, null)
        navController.safeNavigate(action)
    }

    @UiThread
    private fun onMustAcceptTerms() {
        val action = SignInFragmentDirections.actionSignInFragmentToLegalAgreementFragment()
        findNavController().safeNavigate(action)
    }

    @UiThread
    private fun onError(error: SignInOutcome.Error) {
        Timber.d("Error: $error")
        binding.formErrorTextView.text = error.formError
        binding.usernameTextInputLayout.error = error.usernameError
        binding.passwordTextInputLayout.error = error.passwordError
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "Error while trying to sign in") // TODO show error message
        binding.formErrorTextView.setText(R.string.unknown_error)
    }
}

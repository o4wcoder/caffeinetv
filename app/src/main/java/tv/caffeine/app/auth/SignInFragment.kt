package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentSignInBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.ui.setOnActionGo
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

class SignInFragment : CaffeineFragment() {

    private lateinit var binding: FragmentSignInBinding

    private val signInViewModel by lazy { viewModelProvider.get(SignInViewModel::class.java) }

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
        navController.safeNavigate(R.id.lobbyFragment)
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

sealed class SignInOutcome {
    object Success : SignInOutcome()
    object MFARequired : SignInOutcome()
    object MustAcceptTerms : SignInOutcome()
    class Error(val formError: String?, val usernameError: String?, val passwordError: String?) : SignInOutcome()
    class Failure(val exception: Throwable) : SignInOutcome()
}

class SignInViewModel(
        dispatchConfig: DispatchConfig,
        private val signInUseCase: SignInUseCase
) : CaffeineViewModel(dispatchConfig) {

    fun login(username: String, password: String): LiveData<SignInOutcome> {
        val liveData = MutableLiveData<SignInOutcome>()
        launch {
            val result = signInUseCase(username, password)
            liveData.value = when(result) {
                is CaffeineResult.Success -> processSuccess(result.value)
                is CaffeineResult.Error -> processError(result.error)
                is CaffeineResult.Failure -> processFailure(result.throwable)
            }
        }
        return Transformations.map(liveData) { it }
    }

    private fun processSuccess(signInResult: SignInResult) =
            when(signInResult.next) {
                NextAccountAction.mfa_otp_required -> SignInOutcome.MFARequired
                NextAccountAction.legal_acceptance_required -> SignInOutcome.MustAcceptTerms
                else -> SignInOutcome.Success
            }

    private fun processError(error: ApiErrorResult): SignInOutcome {
        val formError = error.generalErrorsString
        val usernameError = error.usernameErrorsString
        val passwordError = error.passwordErrorsString
        return SignInOutcome.Error(formError, usernameError, passwordError)
    }

    private fun processFailure(exception: Throwable) = SignInOutcome.Failure(exception)
}

class SignInUseCase @Inject constructor(
        private val gson: Gson,
        private val accountsService: AccountsService,
        private val tokenStore: TokenStore,
        private val authWatcher: AuthWatcher
) {

    suspend operator fun invoke(username: String, password: String): CaffeineResult<SignInResult> {
        val signInBody = SignInBody(Account(username, password))
        val result = accountsService.signIn(signInBody).awaitAndParseErrors(gson)
        postLogin(result)
        return result
    }

    private fun postLogin(result: CaffeineResult<SignInResult>) {
        if (result !is CaffeineResult.Success) return
        val signInResult = result.value
        if (signInResult.next == null || signInResult.next == NextAccountAction.legal_acceptance_required || signInResult.next == NextAccountAction.email_verification) {
            tokenStore.storeSignInResult(signInResult)
            authWatcher.onSignIn()
        }
    }

}

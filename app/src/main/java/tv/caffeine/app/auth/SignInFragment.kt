package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
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
import tv.caffeine.app.ui.setOnActionGo
import javax.inject.Inject

class SignInFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var gson: Gson
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var authWatcher: AuthWatcher

    private lateinit var binding: FragmentSignInBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.forgotButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.forgotFragment))
        binding.signInButton.setOnClickListener { login() }
        binding.passwordEditText.setOnActionGo { login() }
    }

    private fun login() {
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        binding.formErrorTextView.text = null
        val signInBody = SignInBody(Account(username, password))
        launch {
            val rawResult = runCatching { accountsService.signIn(signInBody).awaitAndParseErrors(gson) }
            val result = rawResult.getOrNull() ?: return@launch onFailure(rawResult.exceptionOrNull() ?: Exception())
            when(result) {
                is CaffeineResult.Success -> onSuccess(result.value)
                is CaffeineResult.Error -> onError(result.error)
                is CaffeineResult.Failure -> onFailure(result.exception)
            }
        }
    }

    @UiThread
    private fun onSuccess(signInResult: SignInResult) {
        val navController = findNavController()
        when(signInResult.next) {
            NextAccountAction.mfa_otp_required -> {
                val username = binding.usernameEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                val action = SignInFragmentDirections.actionSignInFragmentToMfaCodeFragment(username, password, null, null)
                navController.navigate(action)
            }
            else -> {
                tokenStore.storeSignInResult(signInResult)
                navController.popBackStack(R.id.landingFragment, true)
                navController.navigate(R.id.lobbyFragment)
                authWatcher.onSignIn()
            }
        }
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        Timber.d("Error: $error")
        error.errors._error?.joinToString("\n")?.let { binding.formErrorTextView.text = it }
        error.errors.username?.joinToString("\n")?.let { binding.usernameTextInputLayout.error = it }
        error.errors.password?.joinToString("\n")?.let { binding.passwordTextInputLayout.error = it }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "Error while trying to sign in") // TODO show error message
        binding.formErrorTextView.setText(R.string.unknown_error)
    }
}

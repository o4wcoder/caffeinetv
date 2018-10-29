package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.databinding.FragmentSignInBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnActionGo
import javax.inject.Inject

class SignInFragment : CaffeineFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var gson: Gson
    @Inject lateinit var tokenStore: TokenStore
    private lateinit var binding: FragmentSignInBinding
    @Inject lateinit var authWatcher: AuthWatcher

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            val response = accountsService.signIn(signInBody).await()
            withContext(Dispatchers.Main) {
                val signInResult = response.body()
                when {
                    response.isSuccessful && signInResult != null -> onSuccess(signInResult)
                    else -> onError(response)
                }
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
    private fun onError(response: Response<SignInResult>) {
        val signInError = response.errorBody() ?: return
        val error = gson.fromJson(signInError.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        error.errors._error?.joinToString("\n")?.let { binding.formErrorTextView.text = it }
        error.errors.username?.joinToString("\n")?.let { binding.usernameTextInputLayout.error = it }
        error.errors.password?.joinToString("\n")?.let { binding.passwordTextInputLayout.error = it }
    }
}

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
import okhttp3.ResponseBody
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.forgotButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.forgotFragment))
        binding.signInButton.setOnClickListener {
            login()
        }
        binding.passwordEditText.setOnActionGo { login() }
    }

    private fun login() {
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        binding.formErrorTextView.text = null
        val signInBody = SignInBody(Account(username, password))
        launch {
            val request = accountsService.signIn(signInBody).await()
            withContext(Dispatchers.Main) {
                when {
                    request.isSuccessful -> onSuccess(request.body()!!)
                    else -> onError(request.errorBody()!!)
                }
            }
        }
    }

    @UiThread
    private fun onSuccess(signInResult: SignInResult) {
        val navController = findNavController()
        when(signInResult.next) {
            "mfa_otp_required" -> {
                val username = binding.usernameEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                val action = SignInFragmentDirections.actionSignInFragmentToMfaCodeFragment(username, password)
                navController.navigate(action)
            }
            else -> {
                tokenStore.storeSignInResult(signInResult)
                navController.popBackStack(R.id.landingFragment, true)
                navController.navigate(R.id.lobbyFragment)
            }
        }
    }

    @UiThread
    private fun onError(signInError: ResponseBody) {
        val error = gson.fromJson(signInError.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        error.errors._error?.joinToString("\n")?.let { binding.formErrorTextView.text = it }
        error.errors.username?.joinToString("\n")?.let { binding.usernameTextInputLayout.error = it }
        error.errors.password?.joinToString("\n")?.let { binding.passwordTextInputLayout.error = it }
    }
}

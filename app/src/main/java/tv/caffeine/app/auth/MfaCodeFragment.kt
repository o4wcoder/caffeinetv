package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.databinding.FragmentMfaCodeBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnActionGo
import javax.inject.Inject

class MfaCodeFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var gson: Gson
    private lateinit var binding: FragmentMfaCodeBinding
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var authWatcher: AuthWatcher

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentMfaCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.submitMfaCodeButton.setOnClickListener { submitMfaCode() }
        binding.mfaCodeEditText.setOnActionGo { submitMfaCode() }
    }

    private fun submitMfaCode() {
        val args = MfaCodeFragmentArgs.fromBundle(arguments)
        val username = args.username
        val password = args.password
        val caid = args.caid
        val loginToken = args.loginToken
        launch {
            val mfaCode = MfaCode(binding.mfaCodeEditText.text.toString())
            val signInBody = SignInBody(Account(username, password, caid, loginToken), mfaCode)
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
    private fun onSuccess(result: SignInResult) {
        tokenStore.storeSignInResult(result)
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.navigate(R.id.lobbyFragment)
        authWatcher.onSignIn()
    }

    @UiThread
    private fun onError(response: Response<SignInResult>) {
        val errorBody = response.errorBody() ?: return
        val error = gson.fromJson(errorBody.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        error.errors._error?.joinToString("\n")?.let { binding.formErrorTextView.text = it }
        error.errors.otp?.joinToString("\n")?.let { binding.mfaCodeTextInputLayout.error = it }
    }

}

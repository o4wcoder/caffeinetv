package tv.caffeine.app.auth


import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.databinding.FragmentMfaCodeBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnActionGo
import tv.caffeine.app.util.convertLinks
import javax.inject.Inject

class MfaCodeFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var gson: Gson
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var authWatcher: AuthWatcher

    private lateinit var binding: FragmentMfaCodeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentMfaCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.submitMfaCodeButton.setOnClickListener { submitMfaCode() }
        binding.mfaCodeEditText.setOnActionGo { submitMfaCode() }
        binding.mfaCodeSubtitle.apply {
            text = convertLinks(R.string.mfa_code_subtitle, resources, ::resendEmailSpanFactory)
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun resendEmailSpanFactory(url: String?) = ResendEmailSpan(url)

    private inner class ResendEmailSpan(url: String?) : URLSpan(url) {
        override fun onClick(widget: View) {
            submitMfaCode(true)
        }
    }

    private fun submitMfaCode(skipMfaCode: Boolean = false) {
        val args = MfaCodeFragmentArgs.fromBundle(arguments)
        val username = args.username
        val password = args.password
        val caid = args.caid
        val loginToken = args.loginToken
        launch {
            val mfaCode = if (skipMfaCode) null else MfaCode(binding.mfaCodeEditText.text.toString())
            val signInBody = SignInBody(Account(username, password, caid, loginToken), mfaCode)
            val response = accountsService.signIn(signInBody).await()
            val signInResult = response.body()
            when {
                response.isSuccessful && signInResult != null -> onSuccess(signInResult)
                else -> onError(response)
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
        binding.formErrorTextView.text = error.generalErrorsString
        binding.mfaCodeTextInputLayout.error = error.otpErrorsString
    }

}

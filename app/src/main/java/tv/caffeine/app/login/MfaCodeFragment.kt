package tv.caffeine.app.login

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import androidx.annotation.UiThread
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.api.Account
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.MfaCode
import tv.caffeine.app.api.SignInBody
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.otpErrorsString
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentMfaCodeBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnActionGo
import tv.caffeine.app.util.convertLinks
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class MfaCodeFragment @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson,
    private val tokenStore: TokenStore,
    private val authWatcher: AuthWatcher,
    private val firebaseAnalytics: FirebaseAnalytics
) : CaffeineFragment(R.layout.fragment_mfa_code) {

    private lateinit var binding: FragmentMfaCodeBinding
    private val args by navArgs<MfaCodeFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentMfaCodeBinding.bind(view)
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

    private fun clearErrors() {
        binding.formErrorTextView.apply {
            visibility = if (text.isEmpty()) View.GONE else View.INVISIBLE
        }
    }

    private fun submitMfaCode(skipMfaCode: Boolean = false) {
        clearErrors()
        val username = args.username
        val password = args.password
        val caid = args.caid
        val loginToken = args.loginToken
        launch {
            val mfaCode = if (skipMfaCode) null else MfaCode(binding.mfaCodeEditText.text.toString())
            val signInBody = SignInBody(Account(username, password, caid, loginToken), mfaCode)
            val result = accountsService.signIn(signInBody).awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> onSuccess(result.value)
                is CaffeineResult.Error -> onError(result.error)
                is CaffeineResult.Failure -> onFailure(result.throwable)
            }
        }
    }

    @UiThread
    private fun onSuccess(result: SignInResult) {
        firebaseAnalytics.logEvent(FirebaseEvent.MFASignInSuccess)
        tokenStore.storeSignInResult(result)
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.safeNavigate(R.id.lobbySwipeFragment)
        authWatcher.onSignIn()
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        Timber.d("Error: $error")
        val errorMessages = listOfNotNull(error.generalErrorsString, error.otpErrorsString)
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            binding.formErrorTextView.text = it
            binding.formErrorTextView.isVisible = true
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "sign in failure")
        showSnackbar(R.string.sign_in_failure)
    }
}

package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.UiThread
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class TwitterAuthFragment : CaffeineFragment() {
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var oauthService: OAuthService
    @Inject lateinit var gson: Gson
    @Inject lateinit var authWatcher: AuthWatcher

    private lateinit var webView: WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_twitter_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
        }
        twitterLogin()
    }

    private fun twitterLogin() {
        launch {
            val result = oauthService.authenticateWith(IdentityProvider.twitter).awaitAndParseErrors(gson)
            handle(result, view!!) { oauthResponse ->
                webView.loadUrl(oauthResponse.authUrl)
                launch {
                    val longPollResult = oauthService.longPoll(oauthResponse.longpollUrl).awaitAndParseErrors(gson)
                    handle(longPollResult, view!!) { longPollResponse ->
                        Timber.d("OAuth login success, $longPollResponse")
                        launch {
                            processOAuthCallbackResult(longPollResponse)
                        }
                    }
                }
            }
        }
    }

    private suspend fun processOAuthCallbackResult(result: OAuthCallbackResult) {
        if (result.oauth != null) return continueToSignUp(result)
        when(result.next) {
            null -> return
            NextAccountAction.mfa_otp_required -> continueToMfaCode(result)
            else -> attemptSignIn(result)
        }
    }

    private suspend fun attemptSignIn(oauthCallbackResult: OAuthCallbackResult) {
        val caid = oauthCallbackResult.caid
        val loginToken = oauthCallbackResult.loginToken
        val signInBody = SignInBody(Account(null, null, caid, loginToken))
        val result = accountsService.signIn(signInBody).awaitAndParseErrors(gson)
        when(result) {
            is CaffeineResult.Success -> onSuccess(result.value)
            is CaffeineResult.Error -> onError(result.error)
            is CaffeineResult.Failure -> onFailure(result.throwable)
        }
    }

    private fun continueToSignUp(oauthCallbackResult: OAuthCallbackResult) {
        val action = TwitterAuthFragmentDirections.actionTwitterAuthFragmentToSignUpFragment()
        action.setOauthCallbackResult(oauthCallbackResult)
        findNavController().navigate(action)
    }

    private fun continueToMfaCode(oauthCallbackResult: OAuthCallbackResult) {
        val action = TwitterAuthFragmentDirections.actionTwitterAuthFragmentToMfaCodeFragment(null, null, oauthCallbackResult.caid, oauthCallbackResult.loginToken)
        val options = NavOptions.Builder().setPopUpTo(R.id.landingFragment, false).build()
        findNavController().navigate(action, options)
    }

    @UiThread
    private fun onSuccess(signInResult: SignInResult) {
        val navController = findNavController()
        tokenStore.storeSignInResult(signInResult)
        navController.popBackStack(R.id.landingFragment, true)
        navController.navigate(R.id.lobbyFragment)
        authWatcher.onSignIn()
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        Timber.e("Twitter login error: $error")
        findNavController().popBackStack()
        activity?.showSnackbar(R.string.twitter_login_failed)
    }

    @UiThread
    private fun onFailure(throwable: Throwable) {
        Timber.e(throwable)
        findNavController().popBackStack()
        activity?.showSnackbar(R.string.twitter_login_failed)
    }
}

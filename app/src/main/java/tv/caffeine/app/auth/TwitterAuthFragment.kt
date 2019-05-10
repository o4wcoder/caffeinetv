package tv.caffeine.app.auth

import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.gson.Gson
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.internal.http2.StreamResetException
import tv.caffeine.app.R
import tv.caffeine.app.api.OAuthCallbackResult
import tv.caffeine.app.api.OAuthService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class TwitterAuthForLogin @Inject constructor(
        oauthService: OAuthService,
        gson: Gson
) : TwitterAuthFragment(oauthService, gson) {
    override val twitterAuth: TwitterAuthViewModel by navGraphViewModels(R.id.login) { viewModelFactory }
}

class TwitterAuthForSettings @Inject constructor(
        oauthService: OAuthService,
        gson: Gson
) : TwitterAuthFragment(oauthService, gson) {
    override val twitterAuth: TwitterAuthViewModel by navGraphViewModels(R.id.settings) { viewModelFactory }
}

abstract class TwitterAuthFragment(
        private val oauthService: OAuthService,
        private val gson: Gson
): CaffeineFragment(R.layout.fragment_twitter_auth) {

    private lateinit var webView: WebView
    abstract val twitterAuth: TwitterAuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // prevent the long poll from holding onto the old instance and dismissing it on screen rotation
        retainInstance = true
        job = SupervisorJob()
        twitterAuth.oauthResult
    }

    override fun onDestroy() {
        // TODO: rethink how we cancel jobs, whether we should check isActive, etc.
        job.cancel()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        webView = view.findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
        }
        launch {
            twitterLogin()
        }
    }

    private suspend fun twitterLogin() {
        val result = oauthService.authenticateWith(IdentityProvider.twitter).awaitAndParseErrors(gson)
        when(result) {
            is CaffeineResult.Success -> {
                val oauthResponse = result.value
                webView.loadUrl(oauthResponse.authUrl)
                longPoll(oauthResponse.longpollUrl)
            }
            is CaffeineResult.Error -> {
                twitterAuth.processTwitterOAuthResult(CaffeineResult.Error(result.error))
                findNavController().popBackStack()
            }
            is CaffeineResult.Failure -> {
                twitterAuth.processTwitterOAuthResult(CaffeineResult.Failure(result.throwable))
                findNavController().popBackStack()
            }
        }
    }

    private suspend fun longPoll(longPollUrl: String) {
        var longPollResult: CaffeineResult<OAuthCallbackResult>?
        do {
            if (!isActive) return
            longPollResult = oauthService.longPoll(longPollUrl).awaitAndParseErrors(gson)
        } while(shouldRetry(longPollResult))
        if (!isActive) return
        longPollResult?.let { twitterAuth.processTwitterOAuthResult(it) }
        findNavController().popBackStack()
    }

    private fun shouldRetry(result: CaffeineResult<OAuthCallbackResult>?): Boolean {
        return result is CaffeineResult.Failure && result.throwable is StreamResetException
    }

}

package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.OAuthCallbackResult
import tv.caffeine.app.api.OAuthService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
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
        val viewModel = ViewModelProviders.of(activity!!, viewModelFactory).get(TwitterViewModel::class.java)
        launch {
            val result = oauthService.authenticateWith(IdentityProvider.twitter).awaitAndParseErrors(gson)
            handle(result, view!!) { oauthResponse ->
                webView.loadUrl(oauthResponse.authUrl)
                launch {
                    val longPollResult = oauthService.longPoll(oauthResponse.longpollUrl).awaitAndParseErrors(gson)
                    viewModel.postTwitterOAuthResult(longPollResult)
                    findNavController().popBackStack()
                }
            }
        }
    }

}

class TwitterViewModel(dispatchConfig: DispatchConfig) : CaffeineViewModel(dispatchConfig) {
    private val _twitterOAuthResult = MutableLiveData<CaffeineResult<OAuthCallbackResult>>()
    val twitterOAuthResult = Transformations.map(_twitterOAuthResult) { it }

    fun postTwitterOAuthResult(result: CaffeineResult<OAuthCallbackResult>) {
        _twitterOAuthResult.value = result
    }
}

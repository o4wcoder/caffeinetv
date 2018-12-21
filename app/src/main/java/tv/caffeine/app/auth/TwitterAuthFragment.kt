package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.internal.http2.StreamResetException
import retrofit2.Response
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.OAuthCallbackResult
import tv.caffeine.app.api.OAuthService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.ui.CaffeineDialogFragment
import tv.caffeine.app.ui.DialogActionBar
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TwitterAuthFragment : CaffeineDialogFragment(), CoroutineScope {

    interface Callback {
        fun processTwitterOAuthResult(result: CaffeineResult<OAuthCallbackResult>)
    }

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var oauthService: OAuthService
    @Inject lateinit var gson: Gson
    @Inject lateinit var authWatcher: AuthWatcher
    @Inject lateinit var dispatchConfig: DispatchConfig

    private lateinit var webView: WebView
    private var longPollDeferred: Deferred<Response<OAuthCallbackResult>>? = null

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullscreenDialogTheme)
        // prevent the long poll from holding onto the old dialog and dismissing it on screen rotation
        retainInstance = true
        job = SupervisorJob()
    }

    override fun onDestroy() {
        // TODO: rethink how we cancel jobs, whether we should check isActive, etc.
        longPollDeferred?.cancel()
        job.cancel()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_twitter_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<DialogActionBar>(R.id.action_bar).apply {
            setTitle(getString(R.string.sign_in_with_twitter))
            setDismissListener { dismiss() }
        }
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
            val callback = targetFragment as? Callback
            when(result) {
                is CaffeineResult.Success -> {
                    val oauthResponse = result.value
                    webView.loadUrl(oauthResponse.authUrl)
                    longPoll(oauthResponse.longpollUrl, callback)
                }
                is CaffeineResult.Error -> {
                    callback?.processTwitterOAuthResult(CaffeineResult.Error(result.error))
                    dismiss()
                }
                is CaffeineResult.Failure -> {
                    callback?.processTwitterOAuthResult(CaffeineResult.Failure(result.throwable))
                    dismiss()
                }
            }
        }
    }

    private fun longPoll(longPollUrl: String, callback: Callback?) {
        launch {
            var longPollResult: CaffeineResult<OAuthCallbackResult>?
            do {
                longPollDeferred = oauthService.longPoll(longPollUrl)
                longPollResult = longPollDeferred?.awaitAndParseErrors(gson)
            } while(isActive && shouldRetry(longPollResult))
            longPollResult?.let { callback?.processTwitterOAuthResult(it) }
            dismiss()
        }
    }

    private fun shouldRetry(result: CaffeineResult<OAuthCallbackResult>?): Boolean {
        return result is CaffeineResult.Failure && result.throwable is StreamResetException
    }
}

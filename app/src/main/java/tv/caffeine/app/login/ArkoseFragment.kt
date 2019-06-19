package tv.caffeine.app.login

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.di.ARKOSE_PUBLIC_KEY
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject
import javax.inject.Named

class ArkoseFragment @Inject constructor(
    @Named(ARKOSE_PUBLIC_KEY) private val arkosePublicKey: String
) : CaffeineFragment(R.layout.fragment_arkose) {

    val arkoseViewModel: ArkoseViewModel by navGraphViewModels(R.id.login) { viewModelFactory }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = view.findViewById<WebView>(R.id.arkose_webview)

        webView.apply {
            webChromeClient = arkoseWebChromeClient
            webViewClient = arkoseWebViewClient
            addJavascriptInterface(ArkoseJavaScriptInterface(), "android")
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        }
        webView.settings.apply {
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            setSupportZoom(true)
            displayZoomControls = false
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
        }
        val url = Uri.parse("file:///android_asset/arkose.html").toString()
        webView.loadUrl(url)
    }

    private val arkoseWebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            view?.loadUrl("javascript:setPublicKey(`$arkosePublicKey`)")
        }
    }

    private val arkoseWebChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Timber.d("Console: ${consoleMessage?.message()}")
            return true
        }
    }

    inner class ArkoseJavaScriptInterface {
        @JavascriptInterface
        fun returnToken(arkoseToken: String) {
            activity?.runOnUiThread {
                Timber.d("Arkose token: $arkoseToken")
                arkoseViewModel.processArkoseTokenResult(arkoseToken)
                findNavController().popBackStack()
            }
        }
    }
}

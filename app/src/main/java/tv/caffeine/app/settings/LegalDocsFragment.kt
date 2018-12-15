package tv.caffeine.app.settings


import android.content.Intent
import android.net.MailTo
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.parcel.Parcelize
import tv.caffeine.app.R

@Parcelize
enum class LegalDoc(@StringRes val title: Int, @StringRes val url: Int): Parcelable {
    TOS(R.string.terms_of_service, R.string.url_tos),
    PrivacyPolicy(R.string.privacy_policy, R.string.url_privacy),
    CommunityGuidelines(R.string.community_guidelines, R.string.url_guidelines)
}

class LegalDocsFragment : Fragment() {

    private lateinit var legalDoc: LegalDoc
    // TODO (david): Nick will send me a complete list of external urls that's linked from our website.
    private val urlWhitelist = listOf("caffeine.tv", "adr.org")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        legalDoc = LegalDocsFragmentArgs.fromBundle(arguments).document
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_legal_docs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = view.findViewById<WebView>(R.id.web_view)
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(legalDoc.title)
        webView.settings.apply {
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            setSupportZoom(true)
            displayZoomControls = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.let { url ->
                    val urlString = url.toString()
                    // Handle email links
                    if (urlString.startsWith("mailto:")) {
                        activity?.let {
                            it.startActivity(buildEmailIntent(urlString))
                            return true
                        }
                    }
                    // Ignore other non-network urls
                    if (!URLUtil.isNetworkUrl(urlString)) return true

                    // Handle whitelisted urls
                    for (whitelistedUrl in urlWhitelist) {
                        if (url.host?.endsWith(whitelistedUrl) == true) {
                            return false
                        }
                    }
                }
                return true
            }
        }
        val url = getString(legalDoc.url)
        webView.loadUrl(url)
    }

    private fun buildEmailIntent(url: String): Intent {
        return Intent(Intent.ACTION_SEND).also {
            it.putExtra(Intent.EXTRA_EMAIL, arrayOf(MailTo.parse(url).to))
            it.type = "message/rfc822"
        }
    }
}

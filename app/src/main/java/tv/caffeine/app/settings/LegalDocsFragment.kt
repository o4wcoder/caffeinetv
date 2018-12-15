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
    private val hostWhitelist = listOf(
            "caffeine.tv",
            "policies.google.com" // host matching for Google since they may change their urls
    )
    // The url whitelist should be in sync with https://github.com/caffeinetv/tracer/tree/master/src/static
    // grep -h -r -E -o "href=\"https\:\/\/[^\"]+\"" src/static/ | grep -E -v "(www|images).caffeine.tv"
    // | grep -E -v "https://(fonts)?.google(api)?"
    private val urlWhitelist = listOf(
            "https://www.google.com/intl/en/policies/privacy/", // these two links redirect and they are handled by the host matching above
            "https://www.google.com/intl/en/policies/terms/",
            "https://link.caffeine.tv/discord",
            "https://www.ftc.gov/sites/default/files/documents/one-stops/advertisement-endorsements/091005revisedendorsementguides.pdf",
            "https://www.adr.org",
            "https://suicidepreventionlifeline.org/",
            "https://www.nhs.uk/conditions/suicide/",
            "https://itunes.apple.com/us/app/caffeine-tv-for-gamers/id1170629931",
            "https://www.google.com/chrome/browser/",
            "https://www.mozilla.org/firefox",
            "https://www.google.com/policies/privacy/partners/",
            "https://tools.google.com/dlpage/gaoptout"
    )

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

                    // Match hosts in the whitelist
                    for (host in hostWhitelist) {
                        if (url.host?.endsWith(host) == true) return false
                    }

                    // Match urls in the whitelist
                    if (urlString in urlWhitelist) return false
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

package tv.caffeine.app.settings

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.MailTo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import kotlinx.android.parcel.Parcelize
import tv.caffeine.app.R
import java.io.File

@Parcelize
enum class LegalDoc(@StringRes val title: Int, @StringRes val url: Int): Parcelable {
    TOS(R.string.terms_of_service, R.string.url_tos),
    PrivacyPolicy(R.string.privacy_policy, R.string.url_privacy),
    CommunityGuidelines(R.string.community_guidelines, R.string.url_guidelines)
}

class LegalDocsFragment : WebViewFragment() {
    private lateinit var legalDoc: LegalDoc

    override val hostWhitelist = listOf(
            "caffeine.tv",
            "google.com",
            "apple.com",
            "mozilla.org"
    )

    // The url whitelist should be in sync with https://github.com/caffeinetv/tracer/tree/master/src/static
    // grep -h -r -E -o "href=\"https\:\/\/[^\"]+\"" src/static/ | grep -E -v "(www|images).caffeine.tv"
    // | grep -E -v "https://(fonts)?.google(api)?"
    override val urlWhitelist = listOf(
            "https://www.ftc.gov/sites/default/files/documents/one-stops/advertisement-endorsements/091005revisedendorsementguides.pdf",
            "https://www.adr.org/",
            "https://suicidepreventionlifeline.org/",
            "https://www.nhs.uk/conditions/suicide/"
    )

    override val enableJavaScript = false
    private val args by navArgs<LegalDocsFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        legalDoc = args.document
        webViewTitle = getString(legalDoc.title)
        webViewUrl = getString(legalDoc.url)
    }

}

class CaffeineLinksFragment : WebViewFragment() {

    override val hostWhitelist: List<String> = listOf("caffeine.tv")
    override val urlWhitelist: List<String> = listOf()
    override val enableJavaScript = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webViewTitle = getString(R.string.welcome_to_caffeine)
        webViewUrl = activity?.intent?.data?.toString() ?: "https://www.caffeine.tv/tos.html"
    }

}

sealed class WebViewFragment : Fragment() {

    abstract val hostWhitelist: List<String>
    abstract val urlWhitelist: List<String>
    abstract val enableJavaScript: Boolean

    protected lateinit var webViewTitle: String
    protected lateinit var webViewUrl: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_legal_docs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = view.findViewById<WebView>(R.id.web_view)
        (activity as? AppCompatActivity)?.supportActionBar?.title = webViewTitle
        webView.settings.apply {
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            setSupportZoom(true)
            displayZoomControls = false
            javaScriptEnabled = enableJavaScript
            domStorageEnabled = true
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

                    // Open unsupported and insecure links in Chrome
                    startActivity(buildUrlIntent(url))
                    return true
                }

                return true
            }
        }
        webView.setDownloadListener { url, _, _, _, _ ->
            activity?.let {
                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(it, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(it, permission)) {
                        // Do nothing since it's quite obvious that the user initiates the download.
                    } else {
                        ActivityCompat.requestPermissions(it, arrayOf(permission), 0)
                    }
                } else {
                    DownloadManager.Request(Uri.parse(url)).apply {
                        allowScanningByMediaScanner()
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, File(url).name)
                        it.getSystemService<DownloadManager>()?.enqueue(this)
                    }
                }
            }
        }
        val url = webViewUrl
        webView.loadUrl(url)
    }

    private fun buildEmailIntent(url: String): Intent {
        return Intent(Intent.ACTION_SEND).also {
            it.putExtra(Intent.EXTRA_EMAIL, arrayOf(MailTo.parse(url).to))
            it.type = "message/rfc822"
        }
    }

    private fun buildUrlIntent(uri: Uri) = Intent(Intent.ACTION_VIEW).also { it.data = uri }
}

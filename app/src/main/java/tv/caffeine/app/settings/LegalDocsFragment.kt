package tv.caffeine.app.settings


import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
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
        val url = getString(legalDoc.url)
        webView.loadUrl(url)
    }
}

package tv.caffeine.app.auth

import android.content.res.Resources
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.LegalAcceptanceResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentLegalAgreementBinding
import tv.caffeine.app.settings.LegalDoc
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class LegalAgreementFragment : CaffeineFragment() {

    private lateinit var binding: FragmentLegalAgreementBinding

    private val legalAgreementViewModel by lazy { viewModelProvider.get(LegalAgreementViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLegalAgreementBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.legalAgreementTextView.apply {
            text = buildLegalDocSpannable(findNavController())
            movementMethod = LinkMovementMethod.getInstance()
        }
        binding.agreeButton.setOnClickListener {
            legalAgreementViewModel.agree().observe(viewLifecycleOwner, Observer {  outcome ->
                when(outcome) {
                    is LegalAgreementOutcome.Success -> onSuccess()
                    is LegalAgreementOutcome.Error -> Timber.e(Exception("Error accepting agreement"))
                    is LegalAgreementOutcome.Failure -> Timber.e(outcome.exception)
                }
            })
        }
    }

    @UiThread
    private fun onSuccess() {
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.navigate(R.id.lobbyFragment)
    }

    private fun buildLegalDocSpannable(navController: NavController) : Spannable {
        val spannable = SpannableString(HtmlCompat.fromHtml(
                resources.getString(R.string.must_agree_to_legal), HtmlCompat.FROM_HTML_MODE_LEGACY))
        for (urlSpan in spannable.getSpans<URLSpan>(0, spannable.length, URLSpan::class.java)) {
            spannable.setSpan(LegalDocLinkSpan(urlSpan.url, navController, resources),
                    spannable.getSpanStart(urlSpan),
                    spannable.getSpanEnd(urlSpan),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.removeSpan(urlSpan)
        }
        return spannable
    }

    private class LegalDocLinkSpan(url: String?, val navController: NavController, resources: Resources) : URLSpan(url) {
        val legalDoc = LegalDoc.values().find { resources.getString(it.url) == url }

        override fun onClick(widget: View) {
            legalDoc?.let {
                navController.navigate(LegalAgreementFragmentDirections.actionLegalAgreementFragmentToLegalDocsFragment(it))
            }
        }
    }
}

sealed class LegalAgreementOutcome {
    object Success : LegalAgreementOutcome()
    object Error : LegalAgreementOutcome()
    class Failure(val exception: Throwable) : LegalAgreementOutcome()
}

class LegalAgreementViewModel(
        dispatchConfig: DispatchConfig,
        private val acceptLegalUseCase: AcceptLegalUseCase
) : CaffeineViewModel(dispatchConfig) {

    fun agree(): LiveData<LegalAgreementOutcome> {
        val liveData = MutableLiveData<LegalAgreementOutcome>()
        launch {
            val result = acceptLegalUseCase()
            liveData.value = when(result) {
                is CaffeineResult.Success -> LegalAgreementOutcome.Success
                is CaffeineResult.Error -> LegalAgreementOutcome.Error
                is CaffeineResult.Failure -> LegalAgreementOutcome.Failure(result.exception)
            }
        }
        return Transformations.map(liveData) { it }
    }

}

class AcceptLegalUseCase @Inject constructor(
        private val gson: Gson,
        private val accountsService: AccountsService
) {

    suspend operator fun invoke(): CaffeineResult<LegalAcceptanceResult> {
        val rawResult = runCatching {
            accountsService.acceptLegalAgreement().awaitAndParseErrors(gson)
        }
        return rawResult.getOrDefault(CaffeineResult.Failure(Exception("AcceptLegalUseCase")))
    }

}

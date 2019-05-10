package tv.caffeine.app.login

import android.content.res.Resources
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import androidx.annotation.UiThread
import androidx.fragment.app.viewModels
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
import tv.caffeine.app.util.convertLinks
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

class LegalAgreementFragment : CaffeineFragment(R.layout.fragment_legal_agreement) {

    private lateinit var binding: FragmentLegalAgreementBinding

    private val legalAgreementViewModel: LegalAgreementViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentLegalAgreementBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.legalAgreementTextView.apply {
            text = convertLinks(R.string.must_agree_to_legal, resources, ::legalDocLinkSpanFactory)
            movementMethod = LinkMovementMethod.getInstance()
        }
        binding.agreeButton.setOnClickListener {
            legalAgreementViewModel.agree().observe(viewLifecycleOwner, Observer {  outcome ->
                when(outcome) {
                    is LegalAgreementOutcome.Success -> onSuccess()
                    is LegalAgreementOutcome.Error -> Timber.e("Error accepting agreement")
                    is LegalAgreementOutcome.Failure -> Timber.e(outcome.exception)
                }
            })
        }
    }

    @UiThread
    private fun onSuccess() {
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.safeNavigate(R.id.lobbySwipeFragment)
    }

    private fun legalDocLinkSpanFactory(url: String?) =
            LegalDocLinkSpan(url, findNavController(), resources)

    private class LegalDocLinkSpan(url: String?, val navController: NavController, resources: Resources) : URLSpan(url) {
        val legalDoc = LegalDoc.values().find { resources.getString(it.url) == url }

        override fun onClick(widget: View) {
            legalDoc?.let {
                navController.safeNavigate(LegalAgreementFragmentDirections.actionLegalAgreementFragmentToLegalDocsFragment(it))
            }
        }
    }
}

sealed class LegalAgreementOutcome {
    object Success : LegalAgreementOutcome()
    object Error : LegalAgreementOutcome()
    class Failure(val exception: Throwable) : LegalAgreementOutcome()
}

class LegalAgreementViewModel @Inject constructor(
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
                is CaffeineResult.Failure -> LegalAgreementOutcome.Failure(result.throwable)
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
        return accountsService.acceptLegalAgreement().awaitAndParseErrors(gson)
    }

}

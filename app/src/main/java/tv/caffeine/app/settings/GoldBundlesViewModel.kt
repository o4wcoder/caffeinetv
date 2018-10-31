package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.GoldBundlesPayload
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel

class GoldBundlesViewModel(
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase
) : CaffeineViewModel() {
    val goldBundles: LiveData<CaffeineResult<PaymentsEnvelope<GoldBundlesPayload>>> get() = _goldBundles
    private val _goldBundles = MutableLiveData<CaffeineResult<PaymentsEnvelope<GoldBundlesPayload>>>()

    init {
        load()
    }

    private fun load() {
        launch {
            _goldBundles.value = runCatching { loadGoldBundlesUseCase() }.fold({ it }, { CaffeineResult.Failure(it) })
        }
    }

    fun purchaseGoldBundleUsingCredits(goldBundle: GoldBundle) {
        launch {
            runCatching { purchaseGoldBundleUseCase(goldBundle) }
        }
    }
}

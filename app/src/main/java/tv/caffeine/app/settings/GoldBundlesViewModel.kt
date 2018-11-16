package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.launch
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.GoldBundlesPayload
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

class GoldBundlesViewModel(
        dispatchConfig: DispatchConfig,
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase
) : CaffeineViewModel(dispatchConfig) {
    private val _goldBundles = MutableLiveData<CaffeineResult<PaymentsEnvelope<GoldBundlesPayload>>>()
    val goldBundles: LiveData<CaffeineResult<PaymentsEnvelope<GoldBundlesPayload>>> = Transformations.map(_goldBundles) { it }

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

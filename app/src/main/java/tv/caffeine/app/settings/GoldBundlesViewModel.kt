package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.launch
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.map
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.wallet.WalletRepository

class GoldBundlesViewModel(
        dispatchConfig: DispatchConfig,
        private val walletRepository: WalletRepository,
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase
) : CaffeineViewModel(dispatchConfig) {
    private val _goldBundles = MutableLiveData<CaffeineResult<List<GoldBundle>>>()
    val goldBundles: LiveData<CaffeineResult<List<GoldBundle>>> = Transformations.map(_goldBundles) { it }

    val wallet: LiveData<Wallet> = Transformations.map(walletRepository.wallet) { it }

    init {
        load()
    }

    private fun load() {
        walletRepository.refresh()
        launch {
            _goldBundles.value = loadGoldBundlesUseCase().map { it.payload.goldBundles.state }
        }
    }

    fun purchaseGoldBundleUsingCredits(goldBundleId: String): LiveData<Boolean> {
        val resultLiveData = MutableLiveData<Boolean>()
        launch {
            val result = purchaseGoldBundleUseCase(goldBundleId)
            when (result) {
                is CaffeineResult.Success -> walletRepository.refresh().also { resultLiveData.value = true }
                else -> resultLiveData.value = false
            }
        }
        return Transformations.map(resultLiveData) { it }
    }
}

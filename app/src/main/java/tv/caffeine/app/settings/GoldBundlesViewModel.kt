package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.ProcessPlayStorePurchaseBody
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.map
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.wallet.WalletRepository

class PurchaseStatus(val purchaseToken: String, val result: CaffeineResult<Any>)

class GoldBundlesViewModel(
        dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val walletRepository: WalletRepository,
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase,
        private val paymentsClientService: PaymentsClientService
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

    fun processInAppPurchase(purchase: Purchase): LiveData<PurchaseStatus> {
        val resultLiveData = MutableLiveData<PurchaseStatus>()
        launch {
            Timber.d("Purchased ${purchase.sku}: ${purchase.orderId}, ${purchase.purchaseToken}")
            val body = ProcessPlayStorePurchaseBody(purchase.sku, purchase.purchaseToken)
            val result = paymentsClientService.processPlayStorePurchase(body).awaitAndParseErrors(gson)
            resultLiveData.value = PurchaseStatus(purchase.purchaseToken, result)
        }
        return Transformations.map(resultLiveData) { it }
    }
}

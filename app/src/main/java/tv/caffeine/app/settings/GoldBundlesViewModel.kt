package tv.caffeine.app.settings

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.android.billingclient.api.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.api.model.map
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.wallet.WalletRepository
import kotlin.coroutines.resume

sealed class PurchaseStatus {
    data class GooglePlaySuccess(val purchaseToken: String) : PurchaseStatus()
    object CreditsSuccess : PurchaseStatus()
    object CanceledByUser : PurchaseStatus()
    data class Error(@StringRes val error: Int) : PurchaseStatus()
    data class GooglePlayError(val responseCode: Int) : PurchaseStatus()
}

class GoldBundlesViewModel(
        dispatchConfig: DispatchConfig,
        context: Context,
        private val tokenStore: TokenStore,
        private val walletRepository: WalletRepository,
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase,
        private val processPlayStorePurchaseUseCase: ProcessPlayStorePurchaseUseCase
) : CaffeineViewModel(dispatchConfig) {
    private val _events = MutableLiveData<Event<PurchaseStatus>>()
    val events : LiveData<Event<PurchaseStatus>> = Transformations.map(_events) { it }

    private val _goldBundlesUsingCredits = MutableLiveData<CaffeineResult<List<GoldBundle>>>()
    private val _goldBundlesUsingPlayStore = MutableLiveData<CaffeineResult<List<GoldBundle>>>()

    private fun postPurchaseStatus(purchaseStatus: PurchaseStatus) {
        _events.value = Event(purchaseStatus)
    }

    val wallet: LiveData<Wallet> = Transformations.map(walletRepository.wallet) { it }
    private val billingClient: BillingClient = BillingClientFactory.createBillingClient(context,
            PurchasesUpdatedListener { responseCode, purchases ->
                Timber.d("Connected")
                consumeInAppPurchases(responseCode, purchases)
            })

    init {
        load()
    }

    private fun load() {
        refreshWallet()
        loadGoldBundles()
        billingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Timber.e("Billing service disconnected")
            }

            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    Timber.d("Successfully started billing connection")
                } else {
                    Timber.e("Failed to start billing connection")
                }
            }
        })
    }

    private fun loadGoldBundles() {
        launch {
            val allGoldBundles = loadGoldBundlesUseCase()
                    .map { it.payload.goldBundles.state }
            val listUsingCredits = allGoldBundles.map { it.filter { gb -> gb.usingCredits != null } }
            val listUsingPlayStore = allGoldBundles.map { lookupSkuDetails(it.filter { gb -> gb.usingInAppBilling != null }) }
            withContext(dispatchConfig.main) {
                _goldBundlesUsingCredits.value = listUsingCredits
                _goldBundlesUsingPlayStore.value = listUsingPlayStore
            }
        }
    }

    fun load(buyGoldOption: BuyGoldOption): LiveData<CaffeineResult<List<GoldBundle>>> {
        return when(buyGoldOption) {
            BuyGoldOption.UsingCredits -> _goldBundlesUsingCredits
            BuyGoldOption.UsingPlayStore -> _goldBundlesUsingPlayStore
        }
    }

    private suspend fun lookupSkuDetails(list: List<GoldBundle>): List<GoldBundle> {
        val skuList = list.mapNotNull { it.usingInAppBilling }.map { it.productId }
        val params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build()
        val skuDetailsList = billingClient.querySkuDetails(params)
        Timber.d("Results: $skuDetailsList")
        list.forEach { goldBundle ->
            goldBundle.skuDetails = skuDetailsList.find { it.sku == goldBundle.usingInAppBilling?.productId }
        }
        return list
    }

    private suspend fun BillingClient.querySkuDetails(
            params: SkuDetailsParams
    ): List<SkuDetails> = suspendCancellableCoroutine { continuation ->
        querySkuDetailsAsync(params) { responseCode, skuDetailsList ->
            when(responseCode) {
                BillingClient.BillingResponse.OK -> continuation.resume(skuDetailsList)
                else -> {
                    Timber.e("Error loading SKU details")
                    continuation.resume(listOf())
                }
            }
        }
    }

    private fun refreshWallet() {
        walletRepository.refresh()
    }

    private fun consumeInAppPurchases(responseCode: Int, purchases: List<Purchase>?) {
        when {
            responseCode == BillingClient.BillingResponse.OK && purchases != null -> {
                for (purchase in purchases) {
                    consumeInAppPurchase(purchase)
                }
            }
            responseCode == BillingClient.BillingResponse.USER_CANCELED -> {
                Timber.d("User canceled")
                postPurchaseStatus(PurchaseStatus.CanceledByUser)
            }
            else -> {
                Timber.e("Billing client error $responseCode")
                postPurchaseStatus(PurchaseStatus.GooglePlayError(responseCode))
            }
        }
    }

    private fun consumeInAppPurchase(purchase: Purchase) {
        try {
            billingClient.consumeAsync(purchase.purchaseToken) { responseCode, purchaseToken ->
                when (responseCode) {
                    BillingClient.BillingResponse.OK -> {
                        processInAppPurchase(purchase)
                        Timber.d("Successfully consumed the purchase $purchaseToken")
                    }
                    BillingClient.BillingResponse.USER_CANCELED -> {
                        Timber.d("User canceled purchase")
                        postPurchaseStatus(PurchaseStatus.CanceledByUser)
                    }
                    else -> {
                        Timber.e("Failed to consume the purchase $purchaseToken")
                        postPurchaseStatus(PurchaseStatus.GooglePlayError(responseCode))
                    }
                }
            }
        } catch (nie: NotImplementedError) {
            // This will happen when using BillingX testing library,
            // since v 0.8.0 doesn't implement consuming IAB purchases
            Timber.d("Debug build, using BillingX library, consuming purchases isn't supported")
            processInAppPurchase(purchase)
        } catch (t: Throwable) {
            Timber.e(t)
        }
    }

    fun purchaseGoldBundleUsingPlayStore(activity: Activity, goldBundle: GoldBundle) {
        val sku = goldBundle.skuDetails?.sku ?: return postPurchaseStatus(PurchaseStatus.Error(R.string.error_missing_sku))
        val params = BillingFlowParams.newBuilder().setSku(sku).setAccountId(tokenStore.caid).setType(BillingClient.SkuType.INAPP).build()
        billingClient.launchBillingFlow(activity, params)
    }

    fun purchaseGoldBundleUsingCredits(goldBundleId: String) {
        launch {
            val result = purchaseGoldBundleUseCase(goldBundleId)
            when (result) {
                is CaffeineResult.Success -> {
                    refreshWallet()
                    postPurchaseStatus(PurchaseStatus.CreditsSuccess)
                }
                else -> postPurchaseStatus(PurchaseStatus.Error(R.string.error_buying_gold_using_credits))
            }
        }
    }

    /**
     * We will redeem cached google play payments as a short-term fix for the bug that
     * a network or Caffeine error occurred when we processed the payment.
     */
    fun processRecentlyCachedPurchases() {
        val count = 5 // Only re-process the most recent 5 purchases
        val purchaseResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        if (purchaseResult.responseCode == BillingClient.BillingResponse.OK) {
            for (purchase in purchaseResult.purchasesList.takeLast(count)) {
                processInAppPurchase(purchase)
            }
        }
    }

    private fun processInAppPurchase(purchase: Purchase) = launch {
        Timber.d("Purchased ${purchase.sku}: ${purchase.orderId}, ${purchase.purchaseToken}")
        val result = processPlayStorePurchaseUseCase(purchase.sku, purchase.purchaseToken)
        refreshWallet()
        val purchaseStatus = when(result) {
            is CaffeineResult.Success -> PurchaseStatus.GooglePlaySuccess(purchase.purchaseToken)
            is CaffeineResult.Error -> PurchaseStatus.Error(R.string.cannot_purchase_using_play_store)
            is CaffeineResult.Failure -> PurchaseStatus.Error(R.string.cannot_purchase_using_play_store)
        }
        postPurchaseStatus(purchaseStatus)
    }
}

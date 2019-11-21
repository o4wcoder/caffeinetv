package tv.caffeine.app.settings

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import kotlinx.coroutines.Dispatchers
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
import tv.caffeine.app.wallet.WalletRepository
import javax.inject.Inject
import kotlin.coroutines.resume

sealed class PurchaseStatus {
    data class GooglePlaySuccess(val purchaseToken: String) : PurchaseStatus()
    object CreditsSuccess : PurchaseStatus()
    object CanceledByUser : PurchaseStatus()
    data class Error(@StringRes val error: Int) : PurchaseStatus()
    data class GooglePlayError(val billingResult: BillingResult) : PurchaseStatus()
}

class GoldBundlesViewModel @Inject constructor(
    context: Context,
    billingClientFactory: BillingClientFactory,
    private val tokenStore: TokenStore,
    private val walletRepository: WalletRepository,
    private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
    private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase,
    private val processPlayStorePurchaseUseCase: ProcessPlayStorePurchaseUseCase
) : ViewModel(), BillingClientStateListener {

    private val _events = MutableLiveData<Event<PurchaseStatus>>()
    val events: LiveData<Event<PurchaseStatus>> = _events.map { it }

    private val _goldBundlesUsingCredits = MutableLiveData<CaffeineResult<List<GoldBundle>>>()
    private val _goldBundlesUsingPlayStore = MutableLiveData<CaffeineResult<List<GoldBundle>>>()

    private fun postPurchaseStatus(purchaseStatus: PurchaseStatus) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _events.value = Event(purchaseStatus)
        }
    }

    val wallet: LiveData<Wallet> = walletRepository.wallet.map { it }
    private val billingClient: BillingClient = billingClientFactory.createBillingClient(context,
        PurchasesUpdatedListener { billingResult, purchases ->
            Timber.d("Connected")
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> when (purchases) {
                    null -> Timber.e(Exception("Billing response OK, but purchase list is null"))
                    else -> consumeInAppPurchases(purchases)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Timber.d("User canceled")
                    postPurchaseStatus(PurchaseStatus.CanceledByUser)
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    Timber.e(Exception("Items already owned, processing"))
                    processRecentlyCachedPurchases()
                }
                else -> {
                    Timber.e(Exception("Billing client error $billingResult"))
                    postPurchaseStatus(PurchaseStatus.GooglePlayError(billingResult))
                }
            }
        })

    init {
        load()
    }

    private fun load() {
        refreshWallet()
        connectBillingClient()
    }

    private fun connectBillingClient() {
        if (!billingClient.isReady) billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            Timber.d("Successfully started billing connection")
            loadGoldBundles()
            processRecentlyCachedPurchases()
        } else {
            Timber.e(Exception("Failed to start billing connection"))
        }
    }

    override fun onBillingServiceDisconnected() {
        Timber.e(Exception("Billing service disconnected"))
    }

    private fun loadGoldBundles() {
        viewModelScope.launch {
            val allGoldBundles = loadGoldBundlesUseCase()
                .map { it.payload.goldBundles.state }
            withContext(Dispatchers.Main) {
                _goldBundlesUsingCredits.value = allGoldBundles.map { getGoldBundlesUsingCredits(it) }
                _goldBundlesUsingPlayStore.value = allGoldBundles.map {
                    intersectWithPlayStoreProducts(getGoldBundlesUsingPlayStore(it))
                }
            }
        }
    }

    @VisibleForTesting fun getGoldBundlesUsingCredits(allGoldBundles: List<GoldBundle>): List<GoldBundle> {
        return allGoldBundles.filter { it.availableUsingCredits() }
    }

    @VisibleForTesting fun getGoldBundlesUsingPlayStore(allGoldBundles: List<GoldBundle>): List<GoldBundle> {
        return allGoldBundles.filter { it.availableUsingInAppBilling() }
    }

    fun getGoldBundles(buyGoldOption: BuyGoldOption): LiveData<CaffeineResult<List<GoldBundle>>> {
        return when (buyGoldOption) {
            BuyGoldOption.UsingCredits -> _goldBundlesUsingCredits
            BuyGoldOption.UsingPlayStore -> _goldBundlesUsingPlayStore
        }
    }

    private suspend fun intersectWithPlayStoreProducts(list: List<GoldBundle>): List<GoldBundle> {
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
        return list.filter { it.skuDetails != null }
    }

    private suspend fun BillingClient.querySkuDetails(
        params: SkuDetailsParams
    ): List<SkuDetails> = suspendCancellableCoroutine { continuation ->
        querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> continuation.resume(skuDetailsList)
                else -> {
                    Timber.e(Exception("Error loading SKU details $billingResult"))
                    continuation.resume(listOf())
                }
            }
        }
    }

    private fun refreshWallet() {
        walletRepository.refresh()
    }

    private fun consumeInAppPurchases(purchases: List<Purchase>) {
        purchases.forEach { consumeInAppPurchase(it) }
    }

    private fun consumeInAppPurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .setDeveloperPayload(tokenStore.caid)
            .build()
        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    processInAppPurchase(purchase, purchaseToken)
                    Timber.d("Successfully consumed the purchase $purchaseToken")
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Timber.d("User canceled purchase")
                    postPurchaseStatus(PurchaseStatus.CanceledByUser)
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    Timber.e(Exception("Item already owned, processing purchase"))
                    processInAppPurchase(purchase, purchaseToken)
                }
                else -> {
                    Timber.e(Exception("Failed to consume the purchase $billingResult, $purchaseToken"))
                    postPurchaseStatus(PurchaseStatus.GooglePlayError(billingResult))
                }
            }
        }
    }

    fun purchaseGoldBundleUsingPlayStore(activity: Activity, goldBundle: GoldBundle) {
        val sku = goldBundle.skuDetails ?: return postPurchaseStatus(PurchaseStatus.Error(R.string.error_missing_sku))
        val params = BillingFlowParams.newBuilder()
            .setSkuDetails(sku)
            .setAccountId(tokenStore.caid)
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    fun purchaseGoldBundleUsingCredits(goldBundleId: String) {
        viewModelScope.launch {
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
     * We always check previously purchased items and make sure we consume the one that may have been missed before
     */
    private fun processRecentlyCachedPurchases() {
        val purchaseResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        when (purchaseResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> consumeInAppPurchases(purchaseResult.purchasesList)
            else -> Timber.e(Exception("Error querying recent purchases ${purchaseResult.responseCode}"))
        }
    }

    private fun processInAppPurchase(purchase: Purchase, purchaseToken: String = purchase.purchaseToken) = viewModelScope.launch {
        Timber.d("Purchased: ${purchase.sku}, Order ID: ${purchase.orderId}, Purchase Token: $purchaseToken")
        val result = processPlayStorePurchaseUseCase(purchase.sku, purchaseToken)
        refreshWallet()
        val purchaseStatus = when (result) {
            is CaffeineResult.Success -> PurchaseStatus.GooglePlaySuccess(purchase.purchaseToken)
            is CaffeineResult.Error -> PurchaseStatus.Error(R.string.cannot_purchase_using_play_store)
            is CaffeineResult.Failure -> PurchaseStatus.Error(R.string.cannot_purchase_using_play_store)
        }
        postPurchaseStatus(purchaseStatus)
    }
}

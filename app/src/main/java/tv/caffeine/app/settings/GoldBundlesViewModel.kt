package tv.caffeine.app.settings

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
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
    data class GooglePlayError(val responseCode: Int) : PurchaseStatus()
}

class GoldBundlesViewModel @Inject constructor(
    context: Context,
    private val tokenStore: TokenStore,
    private val walletRepository: WalletRepository,
    private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
    private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase,
    private val processPlayStorePurchaseUseCase: ProcessPlayStorePurchaseUseCase
) : ViewModel(), BillingClientStateListener {

    private val _events = MutableLiveData<Event<PurchaseStatus>>()
    val events: LiveData<Event<PurchaseStatus>> = Transformations.map(_events) { it }

    private val _goldBundlesUsingCredits = MutableLiveData<CaffeineResult<List<GoldBundle>>>()
    private val _goldBundlesUsingPlayStore = MutableLiveData<CaffeineResult<List<GoldBundle>>>()

    private fun postPurchaseStatus(purchaseStatus: PurchaseStatus) {
        _events.value = Event(purchaseStatus)
    }

    val wallet: LiveData<Wallet> = Transformations.map(walletRepository.wallet) { it }
    private val billingClient: BillingClient = BillingClientFactory.createBillingClient(context,
            PurchasesUpdatedListener { responseCode, purchases ->
                Timber.d("Connected")
                when (responseCode) {
                    BillingClient.BillingResponse.OK -> when (purchases) {
                        null -> Timber.e(Exception("Billing response OK, but purchase list is null"))
                        else -> consumeInAppPurchases(purchases)
                    }
                    BillingClient.BillingResponse.USER_CANCELED -> {
                        Timber.d("User canceled")
                        postPurchaseStatus(PurchaseStatus.CanceledByUser)
                    }
                    BillingClient.BillingResponse.ITEM_ALREADY_OWNED -> {
                        Timber.e(Exception("Items already owned, processing"))
                        processRecentlyCachedPurchases()
                    }
                    else -> {
                        Timber.e(Exception("Billing client error $responseCode"))
                        postPurchaseStatus(PurchaseStatus.GooglePlayError(responseCode))
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

    override fun onBillingSetupFinished(responseCode: Int) {
        if (responseCode == BillingClient.BillingResponse.OK) {
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
            val listUsingCredits = allGoldBundles
                    .map { it.filter { gb -> gb.usingCredits != null } }
            val listUsingPlayStore = allGoldBundles
                    .map { lookupSkuDetails(it.filter { gb -> gb.usingInAppBilling != null }) }
            withContext(Dispatchers.Main) {
                _goldBundlesUsingCredits.value = listUsingCredits
                _goldBundlesUsingPlayStore.value = listUsingPlayStore
            }
        }
    }

    fun getGoldBundles(buyGoldOption: BuyGoldOption): LiveData<CaffeineResult<List<GoldBundle>>> {
        return when (buyGoldOption) {
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
            when (responseCode) {
                BillingClient.BillingResponse.OK -> continuation.resume(skuDetailsList)
                else -> {
                    Timber.e(Exception("Error loading SKU details $responseCode"))
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
        billingClient.consumeAsync(purchase.purchaseToken) { responseCode, purchaseToken ->
            when (responseCode) {
                BillingClient.BillingResponse.OK -> {
                    processInAppPurchase(purchase, purchaseToken)
                    Timber.d("Successfully consumed the purchase $purchaseToken")
                }
                BillingClient.BillingResponse.USER_CANCELED -> {
                    Timber.d("User canceled purchase")
                    postPurchaseStatus(PurchaseStatus.CanceledByUser)
                }
                BillingClient.BillingResponse.ITEM_ALREADY_OWNED -> {
                    Timber.e(Exception("Item already owned, processing purchase"))
                    processInAppPurchase(purchase, purchaseToken)
                }
                else -> {
                    Timber.e(Exception("Failed to consume the purchase $responseCode, $purchaseToken"))
                    postPurchaseStatus(PurchaseStatus.GooglePlayError(responseCode))
                }
            }
        }
    }

    fun purchaseGoldBundleUsingPlayStore(activity: Activity, goldBundle: GoldBundle) {
        val sku = goldBundle.skuDetails?.sku ?: return postPurchaseStatus(PurchaseStatus.Error(R.string.error_missing_sku))
        val params = BillingFlowParams.newBuilder()
                .setSku(sku)
                .setAccountId(tokenStore.caid)
                .setType(BillingClient.SkuType.INAPP)
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
            BillingClient.BillingResponse.OK -> consumeInAppPurchases(purchaseResult.purchasesList)
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

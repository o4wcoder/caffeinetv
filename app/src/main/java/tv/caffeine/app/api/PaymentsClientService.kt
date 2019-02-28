package tv.caffeine.app.api

import android.content.res.Resources
import com.android.billingclient.api.SkuDetails
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.di.ASSETS_BASE_URL
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue

interface PaymentsClientService {
    @POST("store/get-digital-items")
    fun getDigitalItems(@Body body: GetDigitalItemsBody): Deferred<Response<PaymentsEnvelope<DigitalItemsPayload>>>

    @POST("store/buy-digital-item")
    fun buyDigitalItem(@Body body: BuyDigitalItemBody): Deferred<Response<PaymentsEnvelope<Any>>>

    @POST("store/get-wallet")
    fun getWallet(@Body body: GetWalletBody): Deferred<Response<PaymentsEnvelope<Wallet>>>

    @POST("store/get-transactions")
    fun getTransactionHistory(@Body body: GetTransactionHistoryBody): Deferred<Response<PaymentsEnvelope<TransactionHistoryPayload>>>

    @POST("store/get-gold-bundles")
    fun getGoldBundles(@Body body: GetGoldBundlesBody): Deferred<Response<PaymentsEnvelope<GoldBundlesPayload>>>

    @POST("store/buy-gold-using-credits")
    fun buyGoldUsingCredits(@Body body: BuyGoldUsingCreditsBody): Deferred<Response<Any>>

    @POST("store/process-play-store-purchase")
    fun processPlayStorePurchase(@Body body: ProcessPlayStorePurchaseBody): Deferred<Response<Any>>
}

class GetDigitalItemsBody

class BuyDigitalItemBody(val id: String, val quantity: Int, val recipient: CAID, val message: String)

class GetWalletBody

class GetTransactionHistoryBody

class GetGoldBundlesBody

class BuyGoldUsingCreditsBody(val id: String)

class PaymentsEnvelope<T>(val cursor: String, val retryIn: Int, val payload: T)

class DigitalItemsPayload(val digitalItemCategories: Any, val digitalItems: PaymentsCollection<DigitalItem>)

class PaymentsCollection<T>(val state: List<T>)

data class DigitalItem(val id: String, val name: String, val pluralName: String, val categoryId: String,
                  val goldCost: Int, val score: Int, val staticImagePath: String,
                  val sceneKitPath: String, val webAssetPath: String) {
    val staticImageUrl get() = "$ASSETS_BASE_URL$staticImagePath"
}

class Wallet(val gold: Int, val credits: Int, val cumulativeCredits: Int, val maintenance: Any)

class TransactionHistoryPayload(val transactions: PaymentsCollection<HugeTransactionHistoryItem>)

class HugeTransactionHistoryItem(val id: String, val type: String, val createdAt: Int, val cost: Int?, val value: Int?, val costCurrencyCode: String?, val bundleId: String?, val quantity: Int?, val recipient: CAID?, val sender: CAID?, val pluralName: String?, val name: String?, val assets: DigitalItemAssets?, val state: String?, val currencyCode: String?)

val HugeTransactionHistoryItem.staticImageUrl get() = assets?.let { "$ASSETS_BASE_URL${it.staticImagePath}" }

fun HugeTransactionHistoryItem.convert(): TransactionHistoryItem? {
    return when {
        type == "Bundle" && costCurrencyCode != null && bundleId != null && cost != null && value != null ->
            TransactionHistoryItem.Bundle(costCurrencyCode, bundleId, id, createdAt, cost, value)
        type == "SendDigitalItem" && quantity != null && recipient != null && pluralName != null && name != null && assets != null && cost != null && value != null ->
            TransactionHistoryItem.SendDigitalItem(quantity, recipient, pluralName, name, assets, id, createdAt, cost, value)
        type == "ReceiveDigitalItem" && quantity != null && sender != null && pluralName != null && name != null && assets != null && cost != null && value != null ->
            TransactionHistoryItem.ReceiveDigitalItem(quantity, sender, pluralName, name, assets, id, createdAt, cost, value)
        type == "CashOut" && state != null && cost != null && value != null ->
            TransactionHistoryItem.CashOut(TransactionHistoryItem.CashOut.State.valueOf(state), id, createdAt, cost, value)
        type == "Adjustment" && currencyCode != null && quantity != null ->
            TransactionHistoryItem.Adjustment(currencyCode, quantity, id, createdAt)
        else -> null
    }
}

sealed class TransactionHistoryItem(val id: String, val createdAt: Int, val cost: Int, val value: Int) {
    abstract fun costString(resources: Resources, numberFormat: NumberFormat, username: String, fontColor: String): String?
    class Bundle(val costCurrencyCode: String, val bundleId: String, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value) {
        override fun costString(resources: Resources, numberFormat: NumberFormat, username: String, fontColor: String): String? {
            return when (costCurrencyCode) {
                "CREDITS" -> resources.getString(R.string.transaction_item_purchased_using_credits, numberFormat.format(value), numberFormat.format(cost))
                else -> {
                    val currencyFormatter = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(costCurrencyCode) }
                    resources.getString(R.string.transaction_item_purchased, numberFormat.format(value), currencyFormatter.format(cost / 100f))
                }
            }
        }
    }

    class SendDigitalItem(val quantity: Int, val recipient: CAID, val pluralName: String, val name: String, val assets: DigitalItemAssets, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value) {
        override fun costString(resources: Resources, numberFormat: NumberFormat, username: String, fontColor: String): String? {
            return when(quantity) {
                1 -> resources.getString(R.string.transaction_item_sent, digitalItemStaticImageUrl, numberFormat.format(cost), username, fontColor)
                else -> resources.getString(R.string.transaction_items_sent, digitalItemStaticImageUrl, numberFormat.format(cost), username, fontColor, numberFormat.format(quantity))
            }
        }
    }

    class ReceiveDigitalItem(val quantity: Int, val sender: CAID, val pluralName: String, val name: String, val assets: DigitalItemAssets, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value) {
        override fun costString(resources: Resources, numberFormat: NumberFormat, username: String, fontColor: String): String? {
            return when(quantity) {
                1 -> resources.getString(R.string.transaction_item_received, username, fontColor, digitalItemStaticImageUrl, numberFormat.format(value))
                else -> resources.getString(R.string.transaction_items_received,  username, fontColor, digitalItemStaticImageUrl, numberFormat.format(value), numberFormat.format(quantity))
            }
        }
    }

    class CashOut(val state: State, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value) {
        enum class State {
            pending, deposited, failed
        }

        override fun costString(resources: Resources, numberFormat: NumberFormat, username: String, fontColor: String): String? {
            val currencyFormatter = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("USD") }
            val stringRes = when(state) {
                TransactionHistoryItem.CashOut.State.pending -> R.string.transaction_item_cashout_pending
                TransactionHistoryItem.CashOut.State.deposited -> R.string.transaction_item_cashout_success
                TransactionHistoryItem.CashOut.State.failed -> R.string.transaction_item_cashout_failed
            }
            return resources.getString(stringRes, numberFormat.format(cost), currencyFormatter.format(value / 100f))
        }
    }

    class Adjustment(val currencyCode: String, val quantity: Int, id: String, createdAt: Int) : TransactionHistoryItem(id, createdAt, 0, 0) { // Adjustment doesn't have cost or value
        override fun costString(resources: Resources, numberFormat: NumberFormat, username: String, fontColor: String): String? {
            val stringRes = when {
                quantity >= 0 -> R.string.transaction_item_adjustment_credit
                else -> R.string.transaction_item_adjustment_debit
            }
            return resources.getString(stringRes, numberFormat.format(quantity.absoluteValue))
        }
    }
}

class DigitalItemAssets(val iosSceneKitPath: String, val webAssetPath: String, val staticImagePath: String)

val TransactionHistoryItem.digitalItemStaticImageUrl get() = when(this) {
    is TransactionHistoryItem.SendDigitalItem -> "$ASSETS_BASE_URL${assets.staticImagePath}"
    is TransactionHistoryItem.ReceiveDigitalItem -> "$ASSETS_BASE_URL${assets.staticImagePath}"
    else -> null

}

class GoldBundlesPayload(val maintenance: MaintenanceInfo, val goldBundles: PaymentsCollection<GoldBundle>, val limits: PurchaseLimitsEnvelope)

class MaintenanceInfo

data class GoldBundle(
        val id: String,
        val amount: Int,
        val score: Int,
        val usingCredits: PurchaseOption.PurchaseWithCredits?,
        val usingStoreKit: PurchaseOption.PurchaseUsingStoreKit?,
        val usingInAppBilling: PurchaseOption.PurchaseUsingInAppBilling?,
        val usingStripe: PurchaseOption.PurchaseUsingStripe?,
        var skuDetails: SkuDetails?
)

sealed class PurchaseOption {
    data class PurchaseWithCredits(val id: String, val cost: Int, val canPurchase: Boolean) : PurchaseOption()
    data class PurchaseUsingStoreKit(val productId: String, val canPurchase: Boolean) : PurchaseOption()
    data class PurchaseUsingInAppBilling(val productId: String, val canPurchase: Boolean) : PurchaseOption()
    data class PurchaseUsingStripe(val id: String, val cost: Int, val canPurchase: Boolean) : PurchaseOption()
}

class PurchaseLimitsEnvelope(val goldBundles: PurchaseLimits)
class PurchaseLimits(val canPurchaseAtLeastOne: Boolean, val displayMessage: DisplayMessage)

class DisplayMessage(val body: String)

class ProcessPlayStorePurchaseBody(val productId: String, val purchaseToken: String)

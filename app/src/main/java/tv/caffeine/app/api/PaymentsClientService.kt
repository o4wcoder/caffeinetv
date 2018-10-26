package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import tv.caffeine.app.R

interface PaymentsClientService {
    @POST("store/get-digital-items")
    fun getDigitalItems(@Body body: GetDigitalItemsBody): Deferred<PaymentsEnvelope<DigitalItemsPayload>>

    @POST("store/get-wallet")
    fun getWallet(@Body body: GetWalletBody): Deferred<PaymentsEnvelope<Wallet>>

    @POST("store/get-transactions")
    fun getTransactionHistory(@Body body: GetTransactionHistoryBody): Deferred<Response<PaymentsEnvelope<TransactionHistoryPayload>>>
}

class GetDigitalItemsBody

class GetWalletBody

class GetTransactionHistoryBody

class PaymentsEnvelope<T>(val cursor: String, val retryIn: Int, val payload: T)

class DigitalItemsPayload(val digitalItemCategories: Any, val digitalItems: PaymentsCollection<DigitalItem>)

class PaymentsCollection<T>(val state: List<T>)

data class DigitalItem(val id: String, val name: String, val pluralName: String, val categoryId: String,
                  val goldCost: Int, val score: Int, val staticImagePath: String,
                  val sceneKitPath: String, val webAssetPath: String) {
    val staticImageUrl get() = "https://assets.caffeine.tv$staticImagePath"
}

class Wallet(val gold: Int, val credits: Int, val cumulativeCredits: Int, val maintenance: Any)

class TransactionHistoryPayload(val transactions: PaymentsCollection<HugeTransactionHistoryItem>)

class HugeTransactionHistoryItem(val id: String, val type: String, val createdAt: Int, val cost: Int?, val value: Int?, val costCurrencyCode: String?, val bundleId: String?, val quantity: Int?, val recipient: String?, val sender: String?, val pluralName: String?, val name: String?, val assets: DigitalItemAssets?, val state: String?, val currencyCode: String?)

val HugeTransactionHistoryItem.staticImageUrl get() = assets?.let { "https://assets.caffeine.tv${it.staticImagePath}" }

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
    class Bundle(val costCurrencyCode: String, val bundleId: String, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value)
    class SendDigitalItem(val quantity: Int, val recipient: String, val pluralName: String, val name: String, val assets: DigitalItemAssets, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value)
    class ReceiveDigitalItem(val quantity: Int, val sender: String, val pluralName: String, val name: String, val assets: DigitalItemAssets, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value)
    class CashOut(val state: State, id: String, createdAt: Int, cost: Int, value: Int) : TransactionHistoryItem(id, createdAt, cost, value) {
        enum class State {
            pending, deposited, failed
        }
    }
    class Adjustment(val currencyCode: String, val quantity: Int, id: String, createdAt: Int) : TransactionHistoryItem(id, createdAt, 0, 0) { // Adjustment doesn't have cost or value
        enum class Kind {
            credit, debit
        }
        val kind: Kind get() = if (quantity >= 0) Kind.credit else Kind.debit
    }
}
class DigitalItemAssets(val iosSceneKitPath: String, val webAssetPath: String, val staticImagePath: String)

val TransactionHistoryItem.digitalItemStaticImageUrl get() = when(this) {
    is TransactionHistoryItem.SendDigitalItem -> "https://assets.caffeine.tv${assets.staticImagePath}"
    is TransactionHistoryItem.ReceiveDigitalItem -> "https://assets.caffeine.tv${assets.staticImagePath}"
    else -> null

}

val TransactionHistoryItem.titleResId: Int get() = when(this) {
    is TransactionHistoryItem.Bundle -> R.string.transaction_title_bundle
    is TransactionHistoryItem.SendDigitalItem -> R.string.transaction_title_send
    is TransactionHistoryItem.ReceiveDigitalItem -> R.string.transaction_title_receive
    is TransactionHistoryItem.CashOut -> when(state) {
        TransactionHistoryItem.CashOut.State.pending -> R.string.transaction_title_cashout_pending
        TransactionHistoryItem.CashOut.State.deposited -> R.string.transaction_title_cashout_success
        TransactionHistoryItem.CashOut.State.failed -> R.string.transaction_title_cashout_failed
    }
    is TransactionHistoryItem.Adjustment -> when(kind) {
        TransactionHistoryItem.Adjustment.Kind.credit -> R.string.transaction_title_adjustment_credit
        TransactionHistoryItem.Adjustment.Kind.debit -> R.string.transaction_title_adjustment_debit
    }
}

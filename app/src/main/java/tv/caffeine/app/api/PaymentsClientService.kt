package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentsClientService {
    @POST("store/get-digital-items")
    fun getDigitalItems(@Body body: GetDigitalItemsBody): Deferred<PaymentsEnvelope<DigitalItemsPayload>>

    @POST("store/get-wallet")
    fun getWallet(@Body body: GetWalletBody): Deferred<PaymentsEnvelope<Wallet>>
}

class GetDigitalItemsBody

class GetWalletBody

class PaymentsEnvelope<T>(val cursor: String, val retryIn: Int, val payload: T)

class DigitalItemsPayload(val digitalItemCategories: Any, val digitalItems: DigitalItemsCollection)

class DigitalItemsCollection(val state: List<DigitalItem>)

data class DigitalItem(val id: String, val name: String, val pluralName: String, val categoryId: String,
                  val goldCost: Int, val score: Int, val staticImagePath: String,
                  val sceneKitPath: String, val webAssetPath: String) {
    val staticImageUrl get() = "https://assets.caffeine.tv$staticImagePath"
}

class Wallet(val gold: Int, val credits: Int, val cumulativeCredits: Int, val maintenance: Any)

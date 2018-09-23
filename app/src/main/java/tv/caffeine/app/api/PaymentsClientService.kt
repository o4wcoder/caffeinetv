package tv.caffeine.app.api

import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentsClientService {
    @POST("store/get-digital-items")
    fun getDigitalItems(@Body body: GetDigitalItemsBody): Deferred<DigitalItemsEnvelope>
}

class GetDigitalItemsBody

class DigitalItemsEnvelope(val cursor: String, val payload: DigitalItemsPayload, val retryIn: Int)

class DigitalItemsPayload(val digitalItemCategories: Any, val digitalItems: DigitalItemsCollection)

class DigitalItemsCollection(val state: List<DigitalItem>)

data class DigitalItem(val id: String, val name: String, val pluralName: String, val categoryId: String,
                  val goldCost: Int, val score: Int, val staticImagePath: String,
                  val sceneKitPath: String, val webAssetPath: String) {
    val staticImageUrl get() = "https://assets.caffeine.tv$staticImagePath"
}


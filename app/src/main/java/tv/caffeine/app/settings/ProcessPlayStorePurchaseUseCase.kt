package tv.caffeine.app.settings

import com.google.gson.Gson
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.ProcessPlayStorePurchaseBody
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class ProcessPlayStorePurchaseUseCase @Inject constructor(
        private val paymentsClientService: PaymentsClientService,
        private val gson: Gson
) {
    suspend operator fun invoke(sku: String, purchaseToken: String): CaffeineResult<Any> {
        val body = ProcessPlayStorePurchaseBody(sku, purchaseToken)
        return paymentsClientService.processPlayStorePurchase(body).awaitAndParseErrors(gson)
    }

}

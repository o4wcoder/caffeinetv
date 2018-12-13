package tv.caffeine.app.settings

import com.google.gson.Gson
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class PurchaseGoldBundleUseCase @Inject constructor(
        private val paymentsClientService: PaymentsClientService,
        private val gson: Gson
) {
    suspend operator fun invoke(goldBundle: GoldBundle): CaffeineResult<PaymentsEnvelope<GoldBundlesPayload>> {
        val id = goldBundle.usingCredits?.id ?: return CaffeineResult.Error(ApiErrorResult(ApiError(_error = listOf("Invalid gold bundle ID"))))
        return paymentsClientService.buyGoldUsingCredits(BuyGoldUsingCreditsBody(id)).awaitAndParseErrors(gson)
    }

}

package tv.caffeine.app.settings

import com.google.gson.Gson
import tv.caffeine.app.api.BuyGoldUsingCreditsBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class PurchaseGoldBundleUseCase @Inject constructor(
        private val paymentsClientService: PaymentsClientService,
        private val gson: Gson
) {
    suspend operator fun invoke(goldBundleId: String) =
            paymentsClientService.buyGoldUsingCredits(BuyGoldUsingCreditsBody(goldBundleId)).awaitAndParseErrors(gson)

}

package tv.caffeine.app.settings

import com.google.gson.Gson
import tv.caffeine.app.api.GetGoldBundlesBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class LoadGoldBundlesUseCase @Inject constructor(
        private val paymentsClientService: PaymentsClientService,
        private val gson: Gson
) {
    suspend operator fun invoke() =
            paymentsClientService.getGoldBundles(GetGoldBundlesBody()).awaitAndParseErrors(gson)
}

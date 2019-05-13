package tv.caffeine.app.feature

import com.google.gson.Gson
import timber.log.Timber
import tv.caffeine.app.api.FeatureConfigService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class LoadFeatureConfigUseCase @Inject constructor(
    private val featureConfigService: FeatureConfigService,
    private val featureConfig: FeatureConfig,
    private val gson: Gson
) {
    suspend operator fun invoke(feature: Feature) {
        val result = featureConfigService.check(feature).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> featureConfig.updateFeatures(result.value)
            is CaffeineResult.Error -> Timber.e("Error loading feature config: $feature")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }
}
package tv.caffeine.app.feature

import com.google.gson.Gson
import timber.log.Timber
import tv.caffeine.app.api.FeatureConfigService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.settings.SecureSettingsStorage
import javax.inject.Inject

class LoadFeatureConfigUseCase @Inject constructor(
    private val featureConfigService: FeatureConfigService,
    private val featureConfig: FeatureConfig,
    private val gson: Gson,
    private val secureSettingsStorage: SecureSettingsStorage
) {
    suspend operator fun invoke() {
        val result = featureConfigService.load().awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> {
                featureConfig.updateFeatures(result.value)
                // Cache feature config in shared prefs
                secureSettingsStorage.setFeatureConfigData(result.value)
            }
            is CaffeineResult.Error -> Timber.e("Error loading feature config")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }
}

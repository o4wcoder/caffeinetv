package tv.caffeine.app.settings

import android.content.SharedPreferences
import tv.caffeine.app.di.SETTINGS_SHARED_PREFERENCES
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import javax.inject.Inject
import javax.inject.Named

class AutoPlayConfig @Inject constructor(
    private val featureConfig: FeatureConfig,
    @Named(SETTINGS_SHARED_PREFERENCES) private val sharedPreferences: SharedPreferences
) {
    fun isAutoPlayEnabled(displayOrder: Int): Boolean {
        return featureConfig.isFeatureEnabled(Feature.LIVE_IN_THE_LOBBY) &&
            isAutoPlaySettingEnabled() &&
            (displayOrder == 0 || isPlayAllVideosEnabled())
    }

    private fun isAutoPlaySettingEnabled() = sharedPreferences.getBoolean("autoplay", false)

    private fun isPlayAllVideosEnabled() = sharedPreferences.getBoolean("play_all", false)
}

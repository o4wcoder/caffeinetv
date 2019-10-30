package tv.caffeine.app.settings

import android.content.SharedPreferences
import tv.caffeine.app.di.SETTINGS_SHARED_PREFERENCES
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import javax.inject.Inject
import javax.inject.Named

class ReleaseDesignConfig @Inject constructor(
    private val featureConfig: FeatureConfig,
    @Named(SETTINGS_SHARED_PREFERENCES) private val sharedPreferences: SharedPreferences
) {
    fun isReleaseDesignActive(): Boolean {
        return if (featureConfig.isFeatureEnabled(Feature.DEV_OPTIONS)) {
            isReleaseDesignSettingEnabled()
        } else {
            true
        }
    }

    private fun isReleaseDesignSettingEnabled() = sharedPreferences.getBoolean("release_design", true)
}

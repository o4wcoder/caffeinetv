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
        return featureConfig.isFeatureEnabled(Feature.RELEASE_DESIGN) &&
            isReleaseDesignSettingEnabled()
    }

    private fun isReleaseDesignSettingEnabled() = sharedPreferences.getBoolean("release_design", false)
}

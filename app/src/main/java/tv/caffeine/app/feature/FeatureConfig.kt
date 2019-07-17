package tv.caffeine.app.feature

import com.google.gson.annotations.SerializedName
import tv.caffeine.app.settings.SecureSettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureConfig @Inject constructor(
    secureSettingsStorage: SecureSettingsStorage
) {

    private val config = hashMapOf<String, FeatureNode>()
    private operator fun HashMap<String, FeatureNode>.get(feature: Feature) = get(feature.toString())

    init {
        val storedFeatures = secureSettingsStorage.getFeatureConfigData()
        updateFeatures(storedFeatures)
    }

    fun isFeatureEnabled(feature: Feature): Boolean {
        return config[feature]?.group?.let {
            it.startsWith(Group.enabled.name) || it.startsWith(Group.test.name)
        } ?: false
    }

    fun updateFeatures(features: Map<String, FeatureNode>) {
        for ((featureName, featureNode) in features) {
            config[featureName] = featureNode
        }
    }
}

enum class Feature(private val featureName: String) {
    BROADCAST("android_broadcast"),
    REYES_V5("reyes_v5"),
    LIVE_IN_THE_LOBBY("f316"),
    RELEASE_DESIGN("design_v0");

    override fun toString(): String {
        return featureName
    }
}

enum class Group {
    control, enabled, test
}

class FeatureNode(@SerializedName("node") val group: String, val attributes: HashMap<String, String>?)

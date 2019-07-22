package tv.caffeine.app.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import tv.caffeine.app.di.SECURE_SHARED_PREFERENCES
import tv.caffeine.app.feature.FeatureNode
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KProperty

interface SecureSettingsStorage {
    var deviceId: String?
    var featureConfigJson: String?
    var firebaseToken: String?

    fun clear() {
        deviceId = null
        featureConfigJson = null
        firebaseToken = null
    }

    fun getFeatureConfigData(): Map<String, FeatureNode> {
        val jsonString = featureConfigJson
        return try {
            val type: Type = object : TypeToken<Map<String, FeatureNode>>() {}.type
            jsonString?.let {
                Gson().fromJson<Map<String, FeatureNode>>(it, type)
            } ?: emptyMap()
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    fun setFeatureConfigData(features: Map<String, FeatureNode>) {
        featureConfigJson = Gson().toJson(features)
    }
}

class InMemorySecureSettingsStorage(
    override var deviceId: String? = null,
    override var featureConfigJson: String? = null,
    override var firebaseToken: String? = null
) : SecureSettingsStorage

private const val DEVICE_ID = "DEVICE_ID"
private const val FEATURE_CONFIG = "FEATURE_CONFIG"
private const val FIREBASE_TOKEN = "FIREBASE_TOKEN"

class SecureSharedPrefsStore @Inject constructor(
    @Named(SECURE_SHARED_PREFERENCES) sharedPreferences: SharedPreferences
) : SecureSettingsStorage {

    override var deviceId by SecureSharedPrefsDelegate(sharedPreferences, DEVICE_ID)
    override var featureConfigJson by SharedPrefsDelegate(sharedPreferences, FEATURE_CONFIG)
    override var firebaseToken by SharedPrefsDelegate(sharedPreferences, FIREBASE_TOKEN)
}

class SecureSharedPrefsDelegate(private val sharedPreferences: SharedPreferences, private val prefKey: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = sharedPreferences.getString(prefKey, null)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) = sharedPreferences.edit {
        when (value) {
            null -> remove(prefKey)
            else -> putString(prefKey, value)
        }
    }
}
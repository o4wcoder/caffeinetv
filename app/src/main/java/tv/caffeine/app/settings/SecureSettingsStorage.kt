package tv.caffeine.app.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import tv.caffeine.app.di.SECURE_SHARED_PREFERENCES
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KProperty

interface SecureSettingsStorage {
    var deviceId: String?

    fun clear() {
        deviceId = null
    }
}

class InMemorySecureSettingsStorage(
    override var deviceId: String? = null
) : SecureSettingsStorage

private const val DEVICE_ID = "DEVICE_ID"

class SecureSharedPrefsStore @Inject constructor(
    @Named(SECURE_SHARED_PREFERENCES) sharedPreferences: SharedPreferences
) : SecureSettingsStorage {

    override var deviceId by SecureSharedPrefsDelegate(sharedPreferences, DEVICE_ID)
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
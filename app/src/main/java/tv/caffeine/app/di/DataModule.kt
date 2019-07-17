package tv.caffeine.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.facebook.login.LoginManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import dagger.Module
import dagger.Provides
import org.threeten.bp.Clock
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.notifications.NotificationAuthWatcher
import tv.caffeine.app.settings.EncryptedSettingsStorage
import tv.caffeine.app.settings.KeyStoreHelper
import tv.caffeine.app.settings.SecureSettingsStorage
import tv.caffeine.app.settings.SecureSharedPrefsStore
import tv.caffeine.app.settings.SettingsStorage
import tv.caffeine.app.settings.SharedPrefsStorage
import java.security.KeyStore
import javax.inject.Named
import javax.inject.Singleton

const val CAFFEINE_SHARED_PREFERENCES = "caffeine"
const val SETTINGS_SHARED_PREFERENCES = "SETTINGS_SHARED_PREFERENCES"
const val SECURE_SHARED_PREFERENCES = "SECURE_SHARED_PREFERENCES"

@Module(includes = [
    KeyStoreModule::class,
    SharedPreferencesModule::class,
    SettingsStorageModule::class,
    SecureSettingsStorageModule::class,
    AuthWatcherModule::class,
    FeatureConfigModule::class,
    FacebookModule::class,
    FirebaseModule::class,
    ClockModule::class
])
class DataModule

@Module
class KeyStoreModule {
    @Provides
    @Singleton
    fun providesKeyStore(): KeyStore = KeyStoreHelper.defaultKeyStore()
}

@Module
class SharedPreferencesModule {
    @Provides
    @Named(CAFFEINE_SHARED_PREFERENCES)
    fun providesCaffeineSharedPreferences(context: Context): SharedPreferences = context.getSharedPreferences(CAFFEINE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

    @Provides
    @Named(SETTINGS_SHARED_PREFERENCES)
    fun providesSettingsSharedPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Named(SECURE_SHARED_PREFERENCES)
    fun providesSecureSharedPreferences(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
        SECURE_SHARED_PREFERENCES,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

@Module
class SecureSettingsStorageModule {
    @Provides
    @Singleton
    fun providesSecureSettingsStorage(@Named(SECURE_SHARED_PREFERENCES) sharedPreferences: SharedPreferences): SecureSettingsStorage = SecureSharedPrefsStore(sharedPreferences)
}

@Module
class SettingsStorageModule {
    @Provides
    @Singleton
    fun providesSettingsStorage(sharedPrefsStorage: SharedPrefsStorage, keyStoreHelper: KeyStoreHelper): SettingsStorage = EncryptedSettingsStorage(keyStoreHelper, sharedPrefsStorage)
}

@Module
class AuthWatcherModule {
    @Provides
    @Singleton
    fun providesAuthWatcher(notificationAuthWatcher: NotificationAuthWatcher): AuthWatcher = notificationAuthWatcher
}

@Module
class FeatureConfigModule {
    @Provides
    @Singleton
    fun providesFeatureConfig(): FeatureConfig = FeatureConfig()
}

@Module
class FacebookModule {
    @Provides
    fun providesFacebookLoginManager(): LoginManager = LoginManager.getInstance()
}

@Module
class FirebaseModule {
    @Provides
    @Singleton
    fun providesFirebaseAnalytics(context: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun providesFirebaseInstanceId(): FirebaseInstanceId = FirebaseInstanceId.getInstance()
}

@Module
class ClockModule {
    @Provides
    @Singleton
    fun providesClock(): Clock = Clock.systemDefaultZone()
}

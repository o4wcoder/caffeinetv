package tv.caffeine.app.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.notifications.NotificationAuthWatcher
import tv.caffeine.app.settings.EncryptedSettingsStorage
import tv.caffeine.app.settings.KeyStoreHelper
import tv.caffeine.app.settings.SettingsStorage
import tv.caffeine.app.settings.SharedPrefsStorage
import java.security.KeyStore
import javax.inject.Singleton

private const val CAFFEINE_SHARED_PREFERENCES = "caffeine"

@Module(includes = [
    KeyStoreModule::class,
    SharedPreferencesModule::class,
    SettingsStorageModule::class,
    AuthWatcherModule::class,
    FeatureConfigModule::class
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
    fun providesCaffeineSharedPreferences(context: Context): SharedPreferences = context.getSharedPreferences(CAFFEINE_SHARED_PREFERENCES, Context.MODE_PRIVATE)
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

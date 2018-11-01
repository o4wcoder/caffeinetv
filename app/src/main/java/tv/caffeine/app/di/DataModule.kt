package tv.caffeine.app.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.notifications.NotificationAuthWatcher
import tv.caffeine.app.settings.EncryptedSettingsStorage
import tv.caffeine.app.settings.KeyStoreHelper
import tv.caffeine.app.settings.SettingsStorage
import tv.caffeine.app.settings.SharedPrefsStorage
import java.security.KeyStore
import javax.inject.Singleton

const val REFRESH_TOKEN = "REFRESH_TOKEN"
private const val CAFFEINE_SHARED_PREFERENCES = "caffeine"

@Module
class DataModule {

    @Provides
    fun providesContext(application: Application): Context = application

    @Provides
    fun providesCaffeineSharedPreferences(context: Context) = context.getSharedPreferences(CAFFEINE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun providesKeyStore(): KeyStore = KeyStoreHelper.defaultKeyStore()

    @Provides
    @Singleton
    fun providesSettingsStorage(sharedPrefsStorage: SharedPrefsStorage, keyStoreHelper: KeyStoreHelper): SettingsStorage = EncryptedSettingsStorage(keyStoreHelper, sharedPrefsStorage)

    @Provides
    @Singleton
    fun providesAuthWatcher(notificationAuthWatcher: NotificationAuthWatcher): AuthWatcher = notificationAuthWatcher

}

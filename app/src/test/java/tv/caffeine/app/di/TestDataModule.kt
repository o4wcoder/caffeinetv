package tv.caffeine.app.di

import com.facebook.login.LoginManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import dagger.Module
import dagger.Provides
import io.mockk.mockk
import tv.caffeine.app.settings.InMemorySecureSettingsStorage
import tv.caffeine.app.settings.InMemorySettingsStorage
import tv.caffeine.app.settings.SecureSettingsStorage
import tv.caffeine.app.settings.SettingsStorage
import javax.inject.Singleton

@Module(includes = [
    KeyStoreModule::class,
    SharedPreferencesModule::class,
    FakeSettingsStorageModule::class,
    FakeSecureSettingsStorageModule::class,
    AuthWatcherModule::class,
    FeatureConfigModule::class,
    FakeFacebookModule::class,
    FakeFirebaseModule::class,
    ClockModule::class
])
class TestDataModule

@Module
class FakeSecureSettingsStorageModule {
    @Provides
    @Singleton
    fun providesSecureSettingsStorage(): SecureSettingsStorage = InMemorySecureSettingsStorage()
}

@Module
class FakeSettingsStorageModule {
    @Provides
    @Singleton
    fun providesSettingsStorage(): SettingsStorage = InMemorySettingsStorage()
}

@Module
class FakeFacebookModule {
    @Provides
    fun providesFacebookLoginManager(): LoginManager = mockk(relaxed = true)
}

@Module
class FakeFirebaseModule {
    @Provides
    @Singleton
    fun providesFirebaseAnalytics(): FirebaseAnalytics = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providesFirebaseInstanceId(): FirebaseInstanceId = mockk(relaxed = true)
}

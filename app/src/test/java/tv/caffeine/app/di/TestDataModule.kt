package tv.caffeine.app.di

import com.facebook.login.LoginManager
import dagger.Module
import dagger.Provides
import io.mockk.mockk
import tv.caffeine.app.settings.InMemorySettingsStorage
import tv.caffeine.app.settings.SettingsStorage
import javax.inject.Singleton


@Module(includes = [
    KeyStoreModule::class,
    SharedPreferencesModule::class,
    FakeSettingsStorageModule::class,
    AuthWatcherModule::class,
    FeatureConfigModule::class,
    FakeFacebookModule::class
])
class TestDataModule

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

package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import tv.caffeine.app.settings.InMemorySettingsStorage
import tv.caffeine.app.settings.SettingsStorage
import javax.inject.Singleton


@Module(includes = [
    KeyStoreModule::class,
    SharedPreferencesModule::class,
    FakeSettingsStorageModule::class,
    AuthWatcherModule::class,
    FeatureConfigModule::class
])
class TestDataModule

@Module
class FakeSettingsStorageModule {
    @Provides
    @Singleton
    fun providesSettingsStorage(): SettingsStorage = InMemorySettingsStorage()
}

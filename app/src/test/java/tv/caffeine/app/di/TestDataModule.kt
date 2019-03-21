package tv.caffeine.app.di

import android.content.Context
import com.facebook.login.LoginManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
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
    FakeFacebookModule::class,
    FakeFirebaseModule::class
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

@Module
class FakeFirebaseModule {
    @Provides
    @Singleton
    fun providesFirebaseAnalytics(context: Context): FirebaseAnalytics = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providesFirebaseInstanceId(): FirebaseInstanceId = mockk(relaxed = true)
}

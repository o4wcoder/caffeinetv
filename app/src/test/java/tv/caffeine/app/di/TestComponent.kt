package tv.caffeine.app.di

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import tv.caffeine.app.CaffeineApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidInjectionModule::class,
    AndroidSupportInjectionModule::class,
    InjectionModule::class,
    AppModule::class,
    LoggingModule::class,
    TestNetworkModule::class,
    UIModule::class,
    CoroutinesModule::class,
    TestAnalyticsModule::class,
    CaffeineAssistedModule::class,
    TestDataModule::class
])
interface TestComponent : AndroidInjector<CaffeineApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<CaffeineApplication>()
}

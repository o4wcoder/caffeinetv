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
    NetworkModule::class,
    UIModule::class,
    CoroutinesModule::class,
    AnalyticsModule::class,
    CaffeineAssistedModule::class,
    DataModule::class,
    ArkoseConfigModule::class
])
interface CaffeineComponent : AndroidInjector<CaffeineApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<CaffeineApplication>()
}

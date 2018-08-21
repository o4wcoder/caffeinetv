package tv.caffeine.app.di

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import tv.caffeine.app.CaffeineApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidInjectionModule::class,
    InjectionModule::class,
    AppModule::class,
    NetworkModule::class
])
interface CaffeineComponent : AndroidInjector<CaffeineApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<CaffeineApplication>()
//    interface Builder {
//        @BindsInstance
//        fun application(application: Application): Builder
//        fun build(): CaffeineComponent
//    }
}

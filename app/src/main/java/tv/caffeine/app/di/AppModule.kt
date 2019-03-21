package tv.caffeine.app.di

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.appinit.*

@Module(includes = [
    ContextModule::class,
    AppModuleBinds::class
])
class AppModule

@Module
abstract class ContextModule {
    @Binds
    abstract fun application(app: CaffeineApplication): Application

    @Binds
    abstract fun context(application: CaffeineApplication): Context
}

@Module
abstract class AppModuleBinds {
    @Binds @IntoSet abstract fun provideAndroidThreeTenInitializer(bind: AndroidThreeTenInitializer): AppInitializer
    @Binds @IntoSet abstract fun provideAnalyticsInitializer(bind: AnalyticsInitializer): AppInitializer
    @Binds @IntoSet abstract fun provideBillingClientInitializer(bind: BillingClientInitializer): AppInitializer
    @Binds @IntoSet abstract fun provideTimberInitializer(bind: TimberInitializer): AppInitializer
}

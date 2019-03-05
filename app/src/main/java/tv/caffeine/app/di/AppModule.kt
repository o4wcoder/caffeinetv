package tv.caffeine.app.di

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import tv.caffeine.app.CaffeineApplication

@Module
abstract class AppModule {
    @Binds
    abstract fun application(app: CaffeineApplication): Application

    @Binds
    abstract fun context(application: CaffeineApplication): Context
}

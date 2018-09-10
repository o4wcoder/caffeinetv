package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import okhttp3.logging.HttpLoggingInterceptor
import tv.caffeine.app.util.CrashlyticsTree
import javax.inject.Singleton

@Module
class LoggingModule {
    @Provides
    fun providesHttpLoggingLevel() = HttpLoggingInterceptor.Level.BASIC

    @Provides
    @Singleton
    fun providesTimberTree() : Timber.Tree = CrashlyticsTree()

}

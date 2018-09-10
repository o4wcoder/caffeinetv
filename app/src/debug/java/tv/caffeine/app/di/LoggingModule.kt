package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import javax.inject.Singleton

@Module
class LoggingModule {
    @Provides
    fun providesHttpLoggingLevel() = HttpLoggingInterceptor.Level.BODY

    @Provides
    @Singleton
    fun providesTimberTree(): Timber.Tree = Timber.DebugTree()

}

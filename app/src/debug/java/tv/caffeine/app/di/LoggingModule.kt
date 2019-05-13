package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.Loggable
import org.webrtc.Logging
import timber.log.Timber
import tv.caffeine.app.webrtc.WebRtcLogger
import javax.inject.Singleton

@Module
class LoggingModule {
    @Provides
    fun providesHttpLoggingLevel() = HttpLoggingInterceptor.Level.BODY

    @Provides
    @Singleton
    fun providesTimberTree(): Timber.Tree = Timber.DebugTree()

    @Provides
    fun providesWebRtcLogger(): Loggable? = WebRtcLogger()

    @Provides
    fun providesWebRtcLogLevel(): Logging.Severity = Logging.Severity.LS_VERBOSE
}

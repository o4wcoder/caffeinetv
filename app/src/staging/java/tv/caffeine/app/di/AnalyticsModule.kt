package tv.caffeine.app.di

import android.content.Context
import com.kochava.base.Tracker
import dagger.Module
import dagger.Provides
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.KochavaAnalytics
import javax.inject.Named
import javax.inject.Singleton

const val KOCHAVA_APP_GUID = "KOCHAVA_APP_GUID"
const val KOCHAVA_LOG_LEVEL = "KOCHAVA_LOG_LEVEL"

@Module
class AnalyticsModule {

    @Provides
    @Singleton
    fun providesAnalytics(kochavaAnalytics: KochavaAnalytics): Analytics = kochavaAnalytics

    @Provides
    fun providesKochavaConfiguration(
            context: Context,
            @Named(KOCHAVA_APP_GUID) kochavaAppGuid: String,
            @Named(KOCHAVA_LOG_LEVEL) kochavaLogLevel: Int
    ) = Tracker.Configuration(context)
            .setAppGuid(kochavaAppGuid)
            .setLogLevel(kochavaLogLevel)

    @Provides
    @Named(KOCHAVA_APP_GUID)
    fun providesKochavaAppGuid() = "kocaffeine-android-test-ooejsf9g"

    @Provides
    @Named(KOCHAVA_LOG_LEVEL)
    fun providesKochavaLoggingLevel() = Tracker.LOG_LEVEL_DEBUG
}

// Configuration for production
/*

    @Provides
    @Named(KOCHAVA_APP_GUID)
    fun providesKochavaAppGuid() = "kocaffeine-android-prod-8b5w"

    @Provides
    @Named(KOCHAVA_LOG_LEVEL)
    fun providesKochavaLoggingLevel() = Tracker.LOG_LEVEL_NONE

 */

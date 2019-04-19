package tv.caffeine.app.di

import com.kochava.base.Tracker
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class AnalyticsModuleConfig {

    @Provides
    @Named(KOCHAVA_APP_GUID)
    fun providesKochavaAppGuid() = "kocaffeine-android-prod-8b5w"

    @Provides
    @Named(KOCHAVA_LOG_LEVEL)
    fun providesKochavaLoggingLevel() = Tracker.LOG_LEVEL_NONE
}

package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.LogAnalytics
import javax.inject.Singleton

@Module
class AnalyticsModule {

    @Provides
    @Singleton
    fun providesAnalytics(): Analytics = LogAnalytics()

}

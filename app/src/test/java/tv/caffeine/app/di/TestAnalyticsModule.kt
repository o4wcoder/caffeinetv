package tv.caffeine.app.di

import dagger.Binds
import dagger.Module
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.LogAnalytics
import tv.caffeine.app.analytics.LogProfiling
import tv.caffeine.app.analytics.PassThruProfilingInterceptor
import tv.caffeine.app.analytics.Profiling
import tv.caffeine.app.analytics.ProfilingInterceptor
import javax.inject.Singleton

@Module
abstract class TestAnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindsAnalytics(bind: LogAnalytics): Analytics

    @Binds
    @Singleton
    abstract fun bindsProfiling(bind: LogProfiling): Profiling

    @Binds
    @Singleton
    abstract fun bindsProfilingInterceptor(bind: PassThruProfilingInterceptor): ProfilingInterceptor
}
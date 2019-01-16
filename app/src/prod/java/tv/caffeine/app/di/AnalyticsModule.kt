package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import tv.caffeine.app.analytics.*
import javax.inject.Singleton

@Module
class AnalyticsModule {

    @Provides
    @Singleton
    fun providesAnalytics(): Analytics = LogAnalytics()

    @Provides
    @Singleton
    fun providesProfiling(threatMetrixProfiling: ThreatMetrixProfiling): Profiling = threatMetrixProfiling

    @Provides
    fun providesProfilingInterceptor(threatMetrixInterceptor: ThreatMetrixInterceptor): ProfilingInterceptor = threatMetrixInterceptor
}

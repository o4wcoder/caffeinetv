package tv.caffeine.app.di

import android.content.Context
import com.kochava.base.Tracker
import dagger.Binds
import dagger.Module
import dagger.Provides
import tv.caffeine.app.analytics.*
import javax.inject.Named
import javax.inject.Singleton

const val KOCHAVA_APP_GUID = "KOCHAVA_APP_GUID"
const val KOCHAVA_LOG_LEVEL = "KOCHAVA_LOG_LEVEL"

@Module(includes = [AnalyticsModuleConfig::class, AnalyticsModuleBinds::class])
class AnalyticsModule {

    @Provides
    fun providesKochavaConfiguration(
            context: Context,
            @Named(KOCHAVA_APP_GUID) kochavaAppGuid: String,
            @Named(KOCHAVA_LOG_LEVEL) kochavaLogLevel: Int
    ) = Tracker.Configuration(context)
            .setAppGuid(kochavaAppGuid)
            .setLogLevel(kochavaLogLevel)

}

@Module
abstract class AnalyticsModuleBinds {

    @Binds
    @Singleton
    abstract fun bindsAnalytics(bind: KochavaAnalytics): Analytics

    @Binds
    @Singleton
    abstract fun bindsProfiling(bind: ThreatMetrixProfiling): Profiling

    @Binds
    @Singleton
    abstract fun bindsProfilingInterceptor(bind: ThreatMetrixInterceptor): ProfilingInterceptor

}


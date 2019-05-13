package tv.caffeine.app.analytics

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface Profiling {
    fun initialize()
}

@Singleton
class LogProfiling @Inject constructor() : Profiling {
    override fun initialize() {
        Timber.d("initialize")
    }
}

abstract class ProfilingInterceptor : Interceptor

@Singleton
class PassThruProfilingInterceptor @Inject constructor() : ProfilingInterceptor() {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

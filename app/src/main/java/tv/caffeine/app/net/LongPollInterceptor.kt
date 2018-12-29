package tv.caffeine.app.net

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LongPollInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return if (request.header("Long-poll") == null) {
            chain
        } else {
            chain.withReadTimeout(60, TimeUnit.SECONDS)
        }.proceed(request)
    }
}

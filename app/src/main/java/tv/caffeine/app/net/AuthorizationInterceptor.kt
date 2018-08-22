package tv.caffeine.app.net

import okhttp3.Interceptor
import okhttp3.Response

class AuthorizationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().addHeader("X-Client-Type", "ios").build()
        return chain.proceed(request)
    }
}


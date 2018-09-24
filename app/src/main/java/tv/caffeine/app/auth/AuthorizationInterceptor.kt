package tv.caffeine.app.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthorizationInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().run {
            addHeader("X-Client-Type", "ios")
            tokenStore.addHttpHeaders(this)
            build()
        }
        return chain.proceed(request)
    }
}

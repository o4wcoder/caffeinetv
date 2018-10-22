package tv.caffeine.app.net

import okhttp3.Interceptor
import okhttp3.Response
import tv.caffeine.app.auth.TokenStore

class AuthorizationInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().run {
            addHeader("X-Client-Type", "android")
            tokenStore.addHttpHeaders(this)
            build()
        }
        return chain.proceed(request)
    }
}

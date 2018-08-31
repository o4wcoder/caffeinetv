package tv.caffeine.app.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthorizationInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            addHeader("X-Client-Type", "ios")
            tokenStore.accessToken?.let { addHeader("Authorization", "Bearer $it") }
            tokenStore.credential?.let { addHeader("X-Credential", it) }
        }.build()
        return chain.proceed(request)
    }
}

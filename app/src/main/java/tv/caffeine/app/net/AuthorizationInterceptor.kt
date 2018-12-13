package tv.caffeine.app.net

import okhttp3.Interceptor
import okhttp3.Response
import tv.caffeine.app.auth.TokenStore
import java.io.IOException

class AuthorizationInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.header("No-Authentication") == null) {
            request = request.newBuilder().run {
                tokenStore.addHttpHeaders(this)
                build()
            }
        }
        try {
            return chain.proceed(request)
        } catch (e: Exception) {
            throw IOException("AuthorizationInterceptor", e)
        }
    }
}

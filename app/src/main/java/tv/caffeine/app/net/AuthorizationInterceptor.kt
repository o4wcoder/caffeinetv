package tv.caffeine.app.net

import okhttp3.Interceptor
import okhttp3.Response
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorizationInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.header("No-Authentication") == null) {
            request = request.newBuilder().run {
                tokenStore.addHttpHeaders(this)
                build()
            }
        }
        return chain.proceed(request)
    }
}

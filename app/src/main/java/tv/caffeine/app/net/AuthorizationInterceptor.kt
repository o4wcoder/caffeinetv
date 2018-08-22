package tv.caffeine.app.net

import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

class AuthorizationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().addHeader("X-Client-Type", "ios").build()
        val response = chain.proceed(request)
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            //TODO: handle expired tokens
//            throw UnauthorizedException()
        }
        return response
    }
}

class UnauthorizedException : Exception()

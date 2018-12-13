package tv.caffeine.app.net

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import tv.caffeine.app.BuildConfig
import java.io.IOException

class AppMetaDataInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().run {
            header("User-Agent", "Caffeine/${BuildConfig.VERSION_CODE} Android/${Build.VERSION.SDK_INT}")
            header("X-Client-Type", "android")
            header("X-Client-Version", BuildConfig.VERSION_NAME)
            build()
        }
        try {
            return chain.proceed(request)
        } catch (e: Exception) {
            throw IOException("AppMetaDataInterceptor", e)
        }
    }
}

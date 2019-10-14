package tv.caffeine.app.net

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import okhttp3.Interceptor
import okhttp3.Response
import tv.caffeine.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppMetaDataInterceptor @Inject constructor(context: Context) : Interceptor {

    private val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    @VisibleForTesting val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().run {
            header("User-Agent", "Caffeine/$versionCode Android/${Build.VERSION.SDK_INT}")
            header("X-Client-Type", "android")
            header("X-Client-Version", BuildConfig.VERSION_NAME)
            build()
        }
        return chain.proceed(request)
    }
}

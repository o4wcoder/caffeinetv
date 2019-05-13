package tv.caffeine.app.analytics

import android.content.Context
import com.threatmetrix.TrustDefender.Config
import com.threatmetrix.TrustDefender.ProfilingOptions
import com.threatmetrix.TrustDefender.TrustDefender
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatMetrixInterceptor @Inject constructor(
    private val profiling: ThreatMetrixProfiling
) : ProfilingInterceptor() {

    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionId = profiling.sessionId
        val request = chain.request().newBuilder().header("X-TMX-Session-ID", sessionId).build()
        return chain.proceed(request)
    }
}

@Singleton
class ThreatMetrixProfiling @Inject constructor(
    private val context: Context
) : Profiling {
    internal val sessionId: String = UUID.randomUUID().toString()
    private val options: ProfilingOptions = ProfilingOptions().setSessionID(sessionId)

    override fun initialize() {
        TrustDefender.getInstance().init(createConfig(context))
        val profileHandle = TrustDefender.getInstance().doProfileRequest(options) {
            Timber.d("Got the result $it")
        }
        Timber.d("Session ID = $sessionId, session ID from doProfile: ${profileHandle.sessionID}")
    }

    private fun createConfig(context: Context) = Config()
            .setOrgId("6zavcg6b")
            .setFPServer("tma.caffeine.tv")
            .setContext(context)
            .setTimeout(10, TimeUnit.SECONDS) // optional
            .setRegisterForLocationServices(false) // optional
            .setRegisterForPush(false) // optional
}

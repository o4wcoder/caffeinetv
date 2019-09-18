package tv.caffeine.app.domain

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.net.AuthorizationInterceptor
import tv.caffeine.app.settings.InMemorySecureSettingsStorage
import tv.caffeine.app.settings.InMemorySettingsStorage
import java.util.concurrent.TimeUnit

private const val ACCESS_TOKEN = "access_token"
private const val CREDENTIAL = "credential"

class AuthorizationInterceptorTests {

    @MockK(relaxed = true) lateinit var settingsStorage: InMemorySettingsStorage
    @MockK(relaxed = true) lateinit var secureSettingsStorage: InMemorySecureSettingsStorage

    private lateinit var tokenStore: TokenStore
    private lateinit var subject: AuthorizationInterceptor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        tokenStore = TokenStore(settingsStorage, secureSettingsStorage)
        tokenStore.accessToken = ACCESS_TOKEN
        tokenStore.credential = CREDENTIAL
        subject = AuthorizationInterceptor(tokenStore)
    }

    @Test
    fun `the lobby v5 request has the correct credential and client type`() {
        val chain = TestChain()
        subject.intercept(chain)
        val request = chain.requestToProceed
        assertEquals(CREDENTIAL, request?.header("X-Credential"))
        assertEquals("Bearer $ACCESS_TOKEN", request?.header("Authorization"))
        assertEquals("android", request?.header("X-Client-Type"))
    }

    class TestChain : Interceptor.Chain {
        var requestToProceed: Request? = null

        override fun writeTimeoutMillis(): Int {
            TODO("not implemented")
        }

        override fun call(): Call {
            TODO("not implemented")
        }

        override fun proceed(request: Request): Response {
            requestToProceed = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_0)
                .code(0)
                .message("message")
                .headers(request.headers())
                .build()
        }

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
            TODO("not implemented")
        }

        override fun connectTimeoutMillis(): Int {
            TODO("not implemented")
        }

        override fun connection(): Connection? {
            TODO("not implemented")
        }

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
            TODO("not implemented")
        }

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
            TODO("not implemented")
        }

        override fun request(): Request {
            return Request.Builder().url("https://api.caffeine.tv/public/v5/lobby").build()
        }

        override fun readTimeoutMillis(): Int {
            TODO("not implemented")
        }
    }
}
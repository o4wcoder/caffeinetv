package tv.caffeine.app.realtime

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import tv.caffeine.app.util.DispatchConfig
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

private const val STATUS_CODE_NORMAL_CLOSURE = 1000

private const val HEALZ = "\"HEALZ\""
private const val THANKS = "\"THANKS\""

class WebSocketController(
    private val dispatchConfig: DispatchConfig,
    private val tag: String,
    private val url: String,
    private val headers: String
) : WebSocketListener(), CoroutineScope {

    private var webSocket: WebSocket? = null

    private val gsonForHandshake: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    private var messageNumber: Int = 0

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    val channel = Channel<String>()

    init {
        open()
    }

    fun open() {
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, this)
    }

    fun close() {
        webSocket?.close(STATUS_CODE_NORMAL_CLOSURE, null)
        webSocket = null
        channel.close()
        job.cancel()
    }

    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        log("Opened, response = $response")
        webSocket?.send(headers)
        launch {
            while (isActive) {
                delay(TimeUnit.SECONDS.toMillis(15))
                log("About to send a heartbeat")
                webSocket?.send(HEALZ)
            }
        }
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        log("Got message $text")
        if (text == THANKS) return
        when (messageNumber++) {
            0 -> text?.let { gsonForHandshake.fromJson(it, HandshakeMessage::class.java) }
            else -> text?.let {
                launch { channel.send(it) }
            }
        }
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        log("Closing, code = $code, reason = $reason")
    }

    private fun log(text: String) {
        Timber.d("$tag - $text")
    }

    private class HandshakeMessage(val body: String, val compatibilityMode: Boolean, val headers: Map<String, String>, val status: Int)
}

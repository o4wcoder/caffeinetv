package tv.caffeine.app.realtime

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.timeunit.TimeUnit
import okhttp3.*
import timber.log.Timber

private const val STATUS_CODE_NORMAL_CLOSURE = 1000

private const val HEALZ = "\"HEALZ\""
private const val THANKS = "\"THANKS\""

class WebSocketController(private val tag: String): WebSocketListener() {

    private var webSocket: WebSocket? = null

    private val gsonForHandshake: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    private var messageNumber: Int = 0
    private var keepAlive: Job? = null
    private var callback: ((String) -> Unit)? = null
    private var headers: String = ""

    fun open(url: String, headers: String, callback: (String) -> Unit) {
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url(url).build()
        this.headers = headers
        this.callback = callback
        webSocket = okHttpClient.newWebSocket(request, this)
    }

    fun close() {
        webSocket?.close(STATUS_CODE_NORMAL_CLOSURE, null)
        webSocket = null
        keepAlive?.cancel()
        keepAlive = null
        callback = null
    }

    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        log("Opened, response = $response")
        webSocket?.send(headers)
        keepAlive?.cancel()
        keepAlive = GlobalScope.launch(Dispatchers.Default) {
            while(isActive) {
                delay(15, TimeUnit.SECONDS)
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
            else -> text?.let { callback?.invoke(it) }
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
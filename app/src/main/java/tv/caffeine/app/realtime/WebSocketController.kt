package tv.caffeine.app.realtime

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.*
import timber.log.Timber

private const val STATUS_CODE_NORMAL_CLOSURE = 1000

class WebSocketController {

    private var webSocket: WebSocket? = null

    fun connect(url: String, headers: String, callback: (String) -> Unit) {
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url(url).build()
        val listener = Listener(headers, callback)
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun close() {
        webSocket?.close(STATUS_CODE_NORMAL_CLOSURE, null)
    }

    private class Listener(private val headers: String, val callback: (String) -> Unit): WebSocketListener() {
        val gsonForHandshake: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        val gsonForEvents: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        var messageNumber: Int = 0

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            Timber.d("Opened, response = $response")
            webSocket?.send(headers)
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            Timber.d("Got message $text")
            when (messageNumber++) {
                0 -> text?.let { gsonForHandshake.fromJson(it, HandshakeMessage::class.java) }
                1 -> text?.let { callback(it) }
            }
        }

        override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
            Timber.d("Closing, code = $code, reason = $reason")
        }

    }

    private class HandshakeMessage(val body: String, val compatibilityMode: Boolean, val headers: Map<String, String>, val status: Int)
}
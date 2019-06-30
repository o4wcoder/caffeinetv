package tv.caffeine.app.realtime

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val STATUS_CODE_NORMAL_CLOSURE = 1000

private const val HEALZ = "\"HEALZ\""
private const val THANKS = "\"THANKS\""

private class HandshakeMessage(val body: String, val compatibilityMode: Boolean, val headers: Map<String, String>, val status: Int)

fun webSocketFlow(
    tag: String,
    url: String,
    headers: String
) = callbackFlow<String> {
    val okHttpClient = OkHttpClient.Builder().build()
    val request = Request.Builder().url(url).build()
    var messageNumber = 0
    val gsonForHandshake: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    val webSocketListener = object : WebSocketListener() {

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
                else -> text?.let { offer(it) }
            }
        }

        override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
            log("Closing, code = $code, reason = $reason")
        }

        private fun log(text: String) {
            Timber.d("$tag - $text")
        }
    }
    val webSocket: WebSocket = okHttpClient.newWebSocket(request, webSocketListener)

    awaitClose {
        webSocket.close(STATUS_CODE_NORMAL_CLOSURE, null)
    }
}

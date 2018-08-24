package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.*
import timber.log.Timber

class StageHandshake(private val accessToken: String, private val xCredential: String) {
    fun connect(stageIdentifier: String, callback: (WebSocketEvent) -> Unit) {
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url("wss://realtime.caffeine.tv/v2/stages/$stageIdentifier/details").build()
        val headers = """{
                "Headers": {
                    "x-credential" : "$xCredential",
                    "authorization" : "Bearer $accessToken",
                    "X-Client-Type" : "android",
                    "X-Client-Version" : "0"
                }
            }""".trimMargin()
        val listener = StageHandshakeListener(headers, callback)
        okHttpClient.newWebSocket(request, listener)
    }
}

class StageHandshakeListener(private val headers: String, val callback: (WebSocketEvent) -> Unit): WebSocketListener() {
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
            0 -> text?.let { gsonForHandshake.fromJson(it, WebSocketHandshake::class.java) }
            1 -> text?.let {
                val eventEnvelope = gsonForEvents.fromJson(it, WebSocketEventEnvelope::class.java)
                callback(eventEnvelope.v2)
            }
        }
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        Timber.d("Closing, code = $code, reason = $reason")
    }

}

class WebSocketHandshake(val body: String, val compatibilityMode: Boolean, val headers: Map<String, String>, val status: Int)
class WebSocketEventEnvelope(val v2: WebSocketEvent)
class WebSocketEvent(val gameId: String, val hostConnectionQuality: String, val sessionId: String, val state: String, val streams: Array<WebSocketStream>, val title: String)
class WebSocketStream(val capabilities: Map<String, Boolean>, val id: String, val label: String, val type: String)

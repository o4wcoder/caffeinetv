package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.realtime.WebSocketController

class StageHandshake(private val tokenStore: TokenStore) {
    private var webSocketController: WebSocketController? = null
    private val gsonForEvents: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    private var lastEvent: Event? = null

    data class Event(val gameId: String, val sessionId: String, val state: String, val streams: List<Stream>, val title: String) {
        var hostConnectionQuality: String = ""
    }

    data class Stream(val capabilities: Capabilities, val id: String, val label: String, val type: Type) {
        enum class Type { primary, secondary }
        data class Capabilities(val video: Boolean, val audio: Boolean)
    }

    private class EventEnvelope(val v2: Event)

    fun connect(stageIdentifier: String, callback: (Event) -> Unit) {
        val url = "wss://realtime.caffeine.tv/v2/stages/$stageIdentifier/details"
        val headers = tokenStore.webSocketHeader()
        webSocketController = WebSocketController("stg")
        webSocketController?.open(url, headers) {
            val eventEnvelope = gsonForEvents.fromJson(it, EventEnvelope::class.java)
            val event = eventEnvelope.v2
            if (event == lastEvent) return@open
            lastEvent = event
            callback(event)
        }
    }

    fun close() {
        webSocketController?.close()
        webSocketController = null
    }

}

package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import tv.caffeine.app.realtime.WebSocketController

class StageHandshake(private val accessToken: String, private val xCredential: String) {
    private val webSocketController = WebSocketController("stg")
    private val gsonForEvents: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    private var stageInitialized = false

    class Event(val gameId: String, val hostConnectionQuality: String, val sessionId: String, val state: String, val streams: Array<Stream>, val title: String)
    class Stream(val capabilities: Map<String, Boolean>, val id: String, val label: String, val type: String)

    private class EventEnvelope(val v2: Event)

    fun connect(stageIdentifier: String, callback: (Event) -> Unit) {
        val url = "wss://realtime.caffeine.tv/v2/stages/$stageIdentifier/details"
        val headers = """{
                "Headers": {
                    "x-credential" : "$xCredential",
                    "authorization" : "Bearer $accessToken",
                    "X-Client-Type" : "android",
                    "X-Client-Version" : "0"
                }
            }""".trimMargin()
        webSocketController.connect(url, headers) {
            if (stageInitialized) return@connect
            stageInitialized = true
            val eventEnvelope = gsonForEvents.fromJson(it, EventEnvelope::class.java)
            callback(eventEnvelope.v2)
        }
    }

    fun close() {
        webSocketController.close()
    }

}

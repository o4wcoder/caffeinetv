package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.REALTIME_WEBSOCKET_URL
import tv.caffeine.app.realtime.WebSocketController
import tv.caffeine.app.util.DispatchConfig
import kotlin.coroutines.CoroutineContext

class StageHandshake(
        private val dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val stageIdentifier: String
): CoroutineScope {
    private var webSocketController: WebSocketController? = null
    private val gsonForEvents: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    private var lastEvent: Event? = null

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    val channel = Channel<Event>()

    data class Event(val gameId: String, val sessionId: String, val state: String, val streams: List<Stream>, val title: String) {
        var hostConnectionQuality: String = ""
    }

    data class Stream(val capabilities: Capabilities, val id: String, val label: String, val type: Type) {
        enum class Type { primary, secondary }
        data class Capabilities(val video: Boolean, val audio: Boolean)
    }

    private class EventEnvelope(val v2: Event?)

    init {
        connect()
    }

    private fun connect() {
        val url = "$REALTIME_WEBSOCKET_URL/v2/stages/$stageIdentifier/details"
        val headers = tokenStore.webSocketHeader()
        webSocketController = WebSocketController(dispatchConfig, "stg", url, headers)
        launch {
            webSocketController?.channel?.consumeEach {
                val event = parseEvent(it) ?: return@consumeEach
                if (event == lastEvent) return@consumeEach
                lastEvent = event
                channel.send(event)
            }
        }
    }

    private fun parseEvent(input: String): Event? {
        return try {
            val eventEnvelope = gsonForEvents.fromJson(input, EventEnvelope::class.java)
            eventEnvelope.v2
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    fun close() {
        webSocketController?.close()
        webSocketController = null
    }

}

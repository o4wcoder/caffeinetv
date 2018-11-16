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
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.realtime.WebSocketController
import tv.caffeine.app.util.DispatchConfig
import kotlin.coroutines.CoroutineContext

class MessageHandshake(
        private val dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val stageIdentifier: String
): CoroutineScope {
    private var webSocketController: WebSocketController? = null
    private val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    val channel = Channel<Message>()

    init {
        connect()
    }

    private fun connect() {
        val url = "wss://realtime.caffeine.tv/v2/reaper/stages/$stageIdentifier/messages"
        val headers = tokenStore.webSocketHeader()
        webSocketController?.close()
        webSocketController = WebSocketController(dispatchConfig, "msg", url, headers)
        launch {
            webSocketController?.channel?.consumeEach {
                Timber.d("Received message $it")
                val message = gson.fromJson(it, Message::class.java)
                channel.send(message)
            }
        }
    }

    fun close() {
        Timber.d("msg - closing handshake handler")
        webSocketController?.close()
        webSocketController = null
        channel.close()
        job.cancel()
    }

}

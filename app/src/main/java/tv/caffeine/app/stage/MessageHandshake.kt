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
import tv.caffeine.app.api.model.MessageWrapper
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.REALTIME_WEBSOCKET_URL
import tv.caffeine.app.realtime.WebSocketController
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.DispatchConfig
import kotlin.coroutines.CoroutineContext

class MessageHandshake(
        private val dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val stageIdentifier: String
): CoroutineScope {
    private var webSocketController: WebSocketController? = null
    private val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    val channel = Channel<MessageWrapper>()

    init {
        connect()
    }

    private fun connect() {
        val url = "$REALTIME_WEBSOCKET_URL/v2/reaper/stages/$stageIdentifier/messages"
        val headers = tokenStore.webSocketHeader()
        webSocketController?.close()
        webSocketController = WebSocketController(dispatchConfig, "msg", url, headers)
        launch {
            webSocketController?.channel?.consumeEach {
                Timber.d("Received message $it")
                val message = try {
                    gson.fromJson(it, Message::class.java)
                } catch (e: Exception) {
                    Timber.e(e)
                    return@consumeEach
                }
                val creationTime = System.currentTimeMillis()
                val dummyPosition = -1
                val messageAuthorCaid = message.publisher.caid
                val wrapper = MessageWrapper(message, creationTime, lastUpdateTime = creationTime, position = dummyPosition, isFromFollowedUser = followManager.isFollowing(messageAuthorCaid), isFromSelf = tokenStore.caid == messageAuthorCaid)
                channel.send(wrapper)
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

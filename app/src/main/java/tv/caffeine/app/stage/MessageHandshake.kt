package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.MessageWrapper
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.realtime.WebSocketController
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.DispatchConfig
import kotlin.coroutines.CoroutineContext

class MessageHandshake @AssistedInject constructor(
        private val dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val usersService: UsersService,
        private val gson: Gson,
        private val serverConfig: ServerConfig,
        @Assisted private val stageIdentifier: String
): CoroutineScope {

    @AssistedInject.Factory
    interface Factory {
        fun create(stageIdentifier: String): MessageHandshake
    }

    private var webSocketController: WebSocketController? = null
    private val webSocketGson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    val channel = Channel<MessageWrapper>()

    init {
        connect()
    }

    private fun connect() {
        val url = "${serverConfig.realtimeWebSocket}/v2/reaper/stages/$stageIdentifier/messages"
        val caid = tokenStore.caid ?: return
        webSocketController?.close()
        launch {
            val result = usersService.signedUserDetails(caid).awaitAndParseErrors(gson)
            val payload = when(result) {
                is CaffeineResult.Success -> result.value.token
                is CaffeineResult.Error -> return@launch Timber.e("Failed to get signed user details")
                is CaffeineResult.Failure -> return@launch Timber.e(result.throwable)
            }
            val headers = tokenStore.webSocketHeaderAndSignedPayload(payload)
            webSocketController = WebSocketController(dispatchConfig, "msg", url, headers)
            webSocketController?.channel?.consumeEach {
                Timber.d("Received message $it")
                val message = try {
                    webSocketGson.fromJson(it, Message::class.java)
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

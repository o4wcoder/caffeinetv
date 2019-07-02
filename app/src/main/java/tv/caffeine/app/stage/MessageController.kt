package tv.caffeine.app.stage

import androidx.collection.LruCache
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.threeten.bp.Clock
import timber.log.Timber
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.MessageWrapper
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.chat.classify
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.realtime.webSocketFlow
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.putIfAbsent
import javax.inject.Inject

const val MESSAGE_TIMES_MAX_SIZE = 1_000

class MessageController @Inject constructor(
    private val tokenStore: TokenStore,
    private val followManager: FollowManager,
    private val getSignedUserDetailsUseCase: GetSignedUserDetailsUseCase,
    private val serverConfig: ServerConfig,
    private val clock: Clock
) {

    private val webSocketGson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private val messageCreationTimes = LruCache<String, Long>(MESSAGE_TIMES_MAX_SIZE)

    suspend fun connect(stageIdentifier: String): Flow<MessageWrapper> {
        val url = "${serverConfig.realtimeWebSocket}/v2/reaper/stages/$stageIdentifier/messages"
        val caid = tokenStore.caid ?: return makeFailedToConnectFlow()
        val result = getSignedUserDetailsUseCase(caid)
        val payload = when (result) {
            is CaffeineResult.Success -> result.value.token
            is CaffeineResult.Error -> {
                Timber.e("Failed to get signed user details")
                return makeFailedToConnectFlow()
            }
            is CaffeineResult.Failure -> {
                Timber.e(result.throwable)
                return makeFailedToConnectFlow()
            }
        }
        val headers = tokenStore.webSocketHeaderAndSignedPayload(payload)
        return webSocketFlow("msg", url, headers)
            .mapNotNull {
                try {
                    Timber.d("Received message $it")
                    webSocketGson.fromJson(it, Message::class.java)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
            .map { message ->
                val lastUpdateTime = clock.millis()
                val creationTime = messageCreationTimes.get(message.id) ?: lastUpdateTime
                messageCreationTimes.putIfAbsent(message.id, creationTime)
                val dummyPosition = -1
                val messageAuthorCaid = message.publisher.caid
                val wrapper = MessageWrapper(
                    message,
                    creationTime,
                    lastUpdateTime = lastUpdateTime,
                    position = dummyPosition,
                    isFromFollowedUser = followManager.isFollowing(messageAuthorCaid),
                    isFromSelf = tokenStore.caid == messageAuthorCaid,
                    hasMentions = message.classify(followManager).mentionsSelf)
                return@map wrapper
            }
    }

    private fun makeFailedToConnectFlow(): Flow<MessageWrapper> = flowOf()
}

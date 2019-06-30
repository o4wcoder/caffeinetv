package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.realtime.webSocketFlow
import tv.caffeine.app.util.DispatchConfig
import kotlin.coroutines.CoroutineContext

class FriendWatchingEvent(val isViewing: Boolean, val caid: CAID)

class FriendsWatchingController @AssistedInject constructor(
    private val dispatchConfig: DispatchConfig,
    private val tokenStore: TokenStore,
    private val usersService: UsersService,
    private val gson: Gson,
    private val serverConfig: ServerConfig,
    @Assisted private val stageIdentifier: String
) : CoroutineScope {

    @AssistedInject.Factory
    interface Factory {
        fun create(stageIdentifier: String): FriendsWatchingController
    }

    private val webSocketGson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    val channel = Channel<FriendWatchingEvent>()

    init {
        connect()
    }

    private fun connect() {
        val url = "${serverConfig.realtimeWebSocket}/v2/reaper/stages/$stageIdentifier/viewers/followed"
        val caid = tokenStore.caid ?: return
        launch {
            val result = usersService.signedUserDetails(caid).awaitAndParseErrors(gson)
            val payload = when (result) {
                is CaffeineResult.Success -> result.value.token
                is CaffeineResult.Error -> return@launch Timber.e("Failed to get signed user details")
                is CaffeineResult.Failure -> return@launch Timber.e(result.throwable)
            }
            val headers = tokenStore.webSocketHeaderAndSignedPayload(payload)
            webSocketFlow("viewers/followed", url, headers).collect { item ->
                Timber.d("Received message $item")
                try {
                    val friendWatching = webSocketGson.fromJson(item, FriendWatchingEvent::class.java)
                    channel.send(friendWatching)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    fun close() {
        Timber.d("viewers/followed - closing WebSocket controller")
        channel.close()
        job.cancel()
    }
}

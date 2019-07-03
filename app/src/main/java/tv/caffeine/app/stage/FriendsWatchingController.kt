package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.realtime.webSocketFlow
import javax.inject.Inject

class FriendWatchingEvent(val isViewing: Boolean, val caid: CAID)

class FriendsWatchingController @Inject constructor(
    private val tokenStore: TokenStore,
    private val getSignedUserDetailsUseCase: GetSignedUserDetailsUseCase,
    private val serverConfig: ServerConfig
) {

    private val webSocketGson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    suspend fun connect(stageIdentifier: String): Flow<FriendWatchingEvent> {
        val url = "${serverConfig.realtimeWebSocket}/v2/reaper/stages/$stageIdentifier/viewers/followed"
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
        return webSocketFlow("viewers/followed", url, headers)
            .mapNotNull { item ->
                try {
                    Timber.d("Received message $item")
                    webSocketGson.fromJson(item, FriendWatchingEvent::class.java)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
    }

    private fun makeFailedToConnectFlow(): Flow<FriendWatchingEvent> = flowOf()
}

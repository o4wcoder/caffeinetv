package tv.caffeine.app.stage

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import timber.log.Timber
import tv.caffeine.app.api.ApiError
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.map
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.graphql.asFlow
import tv.caffeine.app.net.CaffeineWebSocketFactory
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.stream.StageSubscription
import tv.caffeine.app.stream.type.Capability
import tv.caffeine.app.stream.type.ClientType
import tv.caffeine.app.stream.type.ContentRating
import tv.caffeine.app.stream.type.Role
import tv.caffeine.app.stream.type.SourceConnectionQuality
import javax.inject.Inject

const val OUT_OF_CAPACITY_REYES_V4 = "OutOfCapacity"
const val OUT_OF_CAPACITY_REYES_V5 = "OutOfCapacityError"

class GraphqlStageDirector @Inject constructor(
    private val serverConfig: ServerConfig,
    private val tokenStore: TokenStore
) : StageDirector {

    @ExperimentalCoroutinesApi
    override suspend fun stageConfiguration(
        username: String,
        clientId: String
    ): Flow<CaffeineResult<Map<String, NewReyes.Feed>>> {
        val webSocketUrl = "${serverConfig.realtimeWebSocket}/public/graphql/query"
        val url = "${serverConfig.realtime}/public/graphql/query"
        val okHttpClient = OkHttpClient.Builder().build()
        val webSocketFactory = CaffeineWebSocketFactory(okHttpClient)
        val subscriptionTransportFactory = WebSocketSubscriptionTransport.Factory(webSocketUrl, webSocketFactory)
        val connectionParams = mapOf("X-Credential" to tokenStore.credential())
        val client = ApolloClient.builder()
            .serverUrl(url)
            .subscriptionTransportFactory(subscriptionTransportFactory)
            .subscriptionConnectionParams(connectionParams)
            .logger { priority, message, t, args -> Timber.log(priority, message, t, args) }
            .build()
        val subscription = StageSubscription(
            clientId,
            ClientType.MOBILE,
            Input.fromNullable("android"),
            Input.fromNullable(true),
            username,
            Input.fromNullable(listOf())
        )
        val flow = client.subscribe(subscription).asFlow()
        return flow.map { response ->
            response.asCaffeineResult().map {
                it.toNewReyesFeeds()
            }
        }
    }
}

fun <T> Response<T>.asCaffeineResult(): CaffeineResult<T> {
    val data = data()
    val errorType = if ((data as? StageSubscription.Data)?.stage?.error?.__typename == OUT_OF_CAPACITY_REYES_V5) {
        OUT_OF_CAPACITY_REYES_V4
    } else {
        null
    }
    return when {
        data != null && errorType == null -> CaffeineResult.Success(data)
        else -> {
            val error = ApiErrorResult(ApiError(_error = errors().map { it.toString() }), type = errorType)
            CaffeineResult.Error(error)
        }
    }
}

fun StageSubscription.Data.toNewReyesFeeds(): Map<String, NewReyes.Feed> {
    val feeds = stage?.stage?.feeds?.filterNotNull() ?: return mapOf()
    val contentRating = stage.stage.contentRating
    return feeds
        .map {
            it.toNewReyes(contentRating)
        }
        .associateBy {
            it.id
        }
}

fun StageSubscription.Feed.toNewReyes(contentRating: ContentRating): NewReyes.Feed {
    val feed = NewReyes.Feed(
        id,
        clientId,
        role.toNewReyes(),
        "TODO",
        1.0,
        false,
        sourceConnectionQuality.toNewReyes(),
        NewReyes.Feed.Content(id, NewReyes.Feed.Content.Type.game),
        capabilities.toNewReyes(),
        "TODO",
        "TODO",
        stream!!.toNewReyes(),
        contentRating
    )
    return feed
}

fun Role.toNewReyes(): NewReyes.Feed.Role {
    return when (this) {
        Role.PRIMARY -> NewReyes.Feed.Role.primary
        Role.SECONDARY -> NewReyes.Feed.Role.secondary
        else -> error("Invalid value")
    }
}

fun SourceConnectionQuality.toNewReyes(): NewReyes.Quality? {
    return when (this) {
        SourceConnectionQuality.GOOD -> NewReyes.Quality.GOOD
        SourceConnectionQuality.POOR -> NewReyes.Quality.POOR
        else -> null
    }
}

fun List<Capability?>.toNewReyes(): NewReyes.Feed.Capabilities {
    return NewReyes.Feed.Capabilities(contains(Capability.AUDIO), contains(Capability.VIDEO))
}

fun StageSubscription.Stream.toNewReyes(): NewReyes.Feed.Stream {
    val stream = inlineFragment as StageSubscription.AsViewerStream
    return NewReyes.Feed.Stream(stream.id, stream.url, "TODO", stream.sdpOffer, "")
}
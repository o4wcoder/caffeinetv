package tv.caffeine.app.stage

import com.google.gson.Gson
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.webrtc.*
import timber.log.Timber
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.webrtc.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class NewReyesFeedInfo(
        val connectionInfo: NewReyesConnectionInfo,
        val feed: NewReyes.Feed,
        val streamId: String,
        val role: NewReyes.Feed.Role
)

class NewReyesConnectionInfo(
        val peerConnection: PeerConnection,
        val videoTrack: VideoTrack?,
        val audioTrack: AudioTrack?
)

private const val HEARTBEAT_PERIOD_SECONDS = 15L
private const val STATS_REPORTING_PERIOD_SECONDS = 3L
private const val DEFAULT_RETRY_DELAY_SECONDS = 10L

class NewReyesController @AssistedInject constructor(
        private val dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val realtime: Realtime,
        private val eventsService: EventsService,
        private val peerConnectionFactory: PeerConnectionFactory,
        @Assisted private val username: String
): CoroutineScope {

    @AssistedInject.Factory
    interface Factory {
        fun create(username: String): NewReyesController
    }

    sealed class Error {
        object PeerConnectionError : Error()
        object OutOfCapacity : Error()
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    val feedChannel = Channel<Map<String, NewReyes.Feed>>()
    val connectionChannel = Channel<NewReyesFeedInfo>()
    val stateChangeChannel = Channel<List<StateChange>>()
    val errorChannel = Channel<Error>()

    private val peerConnections: MutableMap<String, PeerConnection> = ConcurrentHashMap()
    private val peerConnectionStreamLabels: MutableMap<String, String> = ConcurrentHashMap()
    private val heartbeatUrls: MutableMap<String, String> = ConcurrentHashMap()
    private val audioTracks: MutableMap<String, AudioTrack> = ConcurrentHashMap()

    init {
        connect()
        heartbeat()
        stats()
    }

    private fun connect() = launch {
        val uuid = UUID.randomUUID().toString()
        var message = NewReyes.Message(client = NewReyes.Client(id = uuid))
        do {
            var retryIn: Long? = null
            val result = realtime.getStage(username, message).awaitAndParseErrors(gson)
            when(result) {
                is CaffeineResult.Success -> {
                    onSuccess(result.value)
                    message = result.value.copy()
                    retryIn = message.retryIn?.toLong()
                }
                is CaffeineResult.Error -> onError(result.error)
                is CaffeineResult.Failure -> onFailure(result.throwable)
            }
            delay(TimeUnit.SECONDS.toMillis(retryIn ?: DEFAULT_RETRY_DELAY_SECONDS))
        } while(shouldContinue(result))
    }

    private fun heartbeat() = launch {
        while(isActive) {
            heartbeatUrls.values.forEach { url ->
                val result = realtime.heartbeat(url, Object()).awaitAndParseErrors(gson)
            }
            delay(TimeUnit.SECONDS.toMillis(HEARTBEAT_PERIOD_SECONDS))
        }
    }

    private fun stats() = launch {
        while(isActive) {
            peerConnections.forEach {
                val streamId = it.key
                val peerConnection = it.value
                peerConnectionStreamLabels[streamId]?.let { streamLabel ->
                    reportStats(peerConnection, "viewerId", streamLabel) // TODO: viewer ID
                }
            }
            delay(TimeUnit.SECONDS.toMillis(STATS_REPORTING_PERIOD_SECONDS))
        }
    }

    sealed class StateChange {
        class FeedRemoved(val feedId: String, val role: NewReyes.Feed.Role, val streamId: String) : StateChange()
        class FeedAdded(val feed: NewReyes.Feed) : StateChange()
        class FeedRoleChanged(val oldRole: NewReyes.Feed.Role, val newFeed: NewReyes.Feed) : StateChange()
        class FeedStreamChanged(val feed: NewReyes.Feed, val role: NewReyes.Feed.Role, val oldStreamId: String, val newStreamId: String) : StateChange()
    }

    private fun diff(oldFeeds: Map<String, NewReyes.Feed>, newFeeds: Map<String, NewReyes.Feed>): List<StateChange> {
        val stateChangeList = mutableListOf<StateChange>()
        val newKeys = newFeeds.keys - oldFeeds.keys
        val removedKeys = oldFeeds.keys - newFeeds.keys
        val sameKeys = newFeeds.keys.intersect(oldFeeds.keys)
        removedKeys.forEach {
            val oldFeed = oldFeeds[it]!!
            stateChangeList.add(StateChange.FeedRemoved(it, oldFeed.role, oldFeed.stream.id))
        }
        newFeeds.filter { it.key in newKeys }.map { it.value }.forEach {
            stateChangeList.add(StateChange.FeedAdded(it))
        }
        newFeeds.filter { it.key in sameKeys }.map { it.value }.forEach { feed ->
            val oldFeed = oldFeeds[feed.id]!!
            val oldStream = oldFeed.stream
            val stateChange = when {
                oldStream.id != feed.stream.id -> StateChange.FeedStreamChanged(feed, feed.role, feed.stream.id, oldStream.id)
                oldFeed.role != feed.role -> StateChange.FeedRoleChanged(oldFeed.role, feed)
                else -> null // TODO same feed/same stream
            }
            stateChange?.let { stateChangeList.add(it) }
        }
        stateChangeList.sortBy {
            when(it) {
                is StateChange.FeedRemoved -> 1
                is StateChange.FeedAdded -> 2
                is StateChange.FeedRoleChanged -> 3
                is StateChange.FeedStreamChanged -> 4
            }
        }
        return stateChangeList.toList()
    }

    private var feeds: Map<String, NewReyes.Feed> = mapOf()

    private suspend fun onSuccess(message: NewReyes.Message) {
        val oldFeeds = feeds
        val newFeeds = message.payload?.feeds ?: mapOf()
        feedChannel.send(newFeeds)
        newFeeds.values.forEach { feed ->
            audioTracks[feed.stream.id]?.let { audioTrack ->
                Timber.d("DIFF: feed volume changed ${feed.id}, new ${feed.volume}")
                audioTrack.setVolume(feed.volume)
            }
        }
        val diff = diff(oldFeeds, newFeeds)
        diff.forEach {
            when(it) {
                is StateChange.FeedRemoved -> Timber.d("DIFF: feed removed ${it.feedId}")
                is StateChange.FeedAdded -> Timber.d("DIFF: feed added ${it.feed.id}")
                is StateChange.FeedRoleChanged -> Timber.d("DIFF: feed role changed ${it.newFeed.id}, old ${it.oldRole} - new ${it.newFeed.role}")
                is StateChange.FeedStreamChanged -> Timber.d("DIFF: feed stream changed ${it.feed.id}, old ${it.oldStreamId} - new ${it.newStreamId}")
            }
        }
        stateChangeChannel.send(diff)
        diff
                .mapNotNull { stateChange ->
                    when (stateChange) {
                        is StateChange.FeedRemoved -> stateChange.streamId
                        is StateChange.FeedStreamChanged -> stateChange.oldStreamId
                        else -> null
                    }
                }
                .forEach { streamId ->
                    peerConnections.remove(streamId)?.dispose()
                    peerConnectionStreamLabels.remove(streamId)
                    audioTracks.remove(streamId)
                    heartbeatUrls.remove(streamId)
                }
        feeds = newFeeds
        diff
                .mapNotNull { stateChange ->
                    when(stateChange) {
                        is StateChange.FeedAdded -> stateChange.feed
                        is StateChange.FeedStreamChanged -> stateChange.feed
                        else -> null
                    }
                }
                .forEach { feed ->
                    val stream = feed.stream
                    connectStream(stream)?.let { connectionInfo ->
                        peerConnections[stream.id] = connectionInfo.peerConnection
                        peerConnectionStreamLabels[stream.id] = feed.streamLabel()
                        connectionInfo.audioTrack?.let {
                            audioTracks[stream.id] = it
                            it.setVolume(feed.volume)
                        }
                        connectionChannel.send(NewReyesFeedInfo(connectionInfo, feed, stream.id, feed.role))
                    }
                }
    }

    fun NewReyes.Feed.streamLabel() = if (content != null) "content" else "camera"

    private suspend fun onError(error: ApiErrorResult) {
        Timber.e("Error: $error")
        if (error.isOutOfCapacityError()) {
            errorChannel.send(Error.OutOfCapacity)
        }
    }

    private fun onFailure(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun shouldContinue(result: CaffeineResult<NewReyes.Message>): Boolean {
        return isActive && (result !is CaffeineResult.Failure || result.throwable !is CancellationException)
    }

    private suspend fun connectStream(stream: NewReyes.Feed.Stream): NewReyesConnectionInfo? {
        val sdpOffer = stream.sdpOffer
        val mediaConstraints = MediaConstraints()
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdpOffer)
        val rtcConfiguration = PeerConnection.RTCConfiguration(listOf())
        val observer = object : SimplePeerConnectionObserver() {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                if (!isActive) {
                    Timber.e("Ice candidate received after closing the stream controller")
                    return
                }
                if (iceCandidate == null) return
                val candidate = IndividualIceCandidate(iceCandidate.sdp, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex)
                val iceCandidates = NewReyes.ConnectToStream(iceCandidates = arrayOf(candidate))
                launch {
                    val result = realtime.connectToStream(stream.url, iceCandidates).awaitAndParseErrors(gson)
                    when (result) {
                        is CaffeineResult.Success -> Timber.d("ICE candidate sent, ${result.value}")
                        is CaffeineResult.Error -> Timber.e("Failed to send ICE candidate")
                        is CaffeineResult.Failure -> Timber.e(result.throwable)
                    }
                }
            }
        }
        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, observer) ?: return null
        if (!peerConnection.setRemoteDescription(sessionDescription)) {
            errorChannel.send(Error.PeerConnectionError)
            return peerConnection.disposeAndReturnNull()
        }
        val localSessionDescription = peerConnection.createAnswer(mediaConstraints) ?: return peerConnection.disposeAndReturnNull()
        val answer = NewReyes.ConnectToStream(answer = localSessionDescription.description)
        val result = realtime.connectToStream(stream.url, answer).awaitAndParseErrors(gson)
        when(result) {
            is CaffeineResult.Success -> {
                Timber.d("Success: ${result.value}")
                val heartbeatUrl = result.value.urls.heartbeat
                heartbeatUrls[stream.id] = heartbeatUrl
            }
            is CaffeineResult.Error -> Timber.d("Error: ${result.error}")
            is CaffeineResult.Failure -> Timber.d("Failure: ${result.throwable}")
        }
        val result2 = peerConnection.setLocalDescription(localSessionDescription)
        return configureConnections(peerConnection)
    }

    fun mute() {
        audioTracks.values.forEach {
            it.setEnabled(false)
        }
    }

    fun unmute() {
        audioTracks.values.forEach {
            it.setEnabled(true)
        }
    }

    fun close() {
        job.cancel()
        feedChannel.close()
        stateChangeChannel.close()
        connectionChannel.close()
        errorChannel.close()
        peerConnections.keys.forEach {
            peerConnections.remove(it)?.dispose()
        }
        peerConnectionStreamLabels.keys.forEach {
            peerConnectionStreamLabels.remove(it)
        }
        audioTracks.keys.forEach {
            audioTracks.remove(it)
        }
        heartbeatUrls.keys.forEach {
            heartbeatUrls.remove(it)
        }
    }

    private fun PeerConnection.disposeAndReturnNull(): NewReyesConnectionInfo? {
        dispose()
        return null
    }

    private fun configureConnections(peerConnection: PeerConnection): NewReyesConnectionInfo {
        val receivers = peerConnection.receivers
        val videoTrack = receivers.find { it.track() is VideoTrack }?.track() as? VideoTrack
        val audioTrack = receivers.find { it.track() is AudioTrack }?.track() as? AudioTrack
        return NewReyesConnectionInfo(peerConnection, videoTrack, audioTrack)
    }

    private val relevantStatsTypes = listOf("inbound-rtp", "candidate-pair", "remote-candidate", "local-candidate", "track")
    private val relevantStatsMetrics = listOf("bytesReceived", "packetsReceived", "packetsLost", "framesDecoded")

    private suspend fun reportStats(peerConnection: PeerConnection, viewerId: String, streamLabel: String) {
        val rtcStats = peerConnection.getStats()
//        sendEventInfo(rtcStats, viewerId)
        sendPerformanceStats(rtcStats, streamLabel)
    }

    private suspend fun sendEventInfo(rtcStats: RTCStatsReport, viewerId: String) {
        val data = collectEventInfo(rtcStats, viewerId)
        val sendEventResult = eventsService.sendEvent(EventBody("webrtc_stats", data = data)).awaitAndParseErrors(gson)
        when (sendEventResult) {
            is CaffeineResult.Success -> Timber.d("Successfully sent event")
            is CaffeineResult.Error -> Timber.e("Error sending event webrtc_stats")
            is CaffeineResult.Failure -> Timber.e(sendEventResult.throwable)
        }
    }

    private suspend fun sendPerformanceStats(rtcStats: RTCStatsReport, streamLabel: String) {
        val stats = collectPerformanceStats(rtcStats, streamLabel)
        val sendStatsResult = eventsService.sendStats(stats).awaitAndParseErrors(gson)
        when (sendStatsResult) {
            is CaffeineResult.Success -> Timber.d("Successfully sent stats")
            is CaffeineResult.Error -> Timber.e("Error sending stats webrtc_stats")
            is CaffeineResult.Failure -> Timber.e(sendStatsResult.throwable)
        }
    }

    private fun collectPerformanceStats(rtcStats: RTCStatsReport, streamLabel: String): CumulativeCounters {
        val statsToSend: List<StatsSnippet> = rtcStats.statsMap
                .filter { it.value != null }
                .filter { it.value.type == "inbound-rtp" }
                .map { stat ->
                    val mediaType = stat.value.members["mediaType"] as String
                    Timber.d("Stats available: ${stat.value.members.keys}")
                    relevantStatsMetrics
                            .filter { stat.value?.members != null }
                            .filter { stat.value.members.containsKey(it) }
                            .mapNotNull { metricName ->
                                Timber.d("Stats available: $metricName = ${stat.value.members[metricName]}")
                                stat.value.members[metricName]?.toString()?.let { metricStringValue ->
                                    // The absolute value of a cumulative counter is irrelevant. Reset if it exceeds the maximum int.
                                    // https://docs.signalfx.com/en/latest/getting-started/concepts/metric-types.html
                                    val metricValue = metricStringValue.toIntOrNull() ?: 0
                                    StatsSnippet(StatsDimensions(mediaType, streamLabel, username), "rtc.$metricName", metricValue)
                                }
                            }
                }
                .flatten()
        val stats = CumulativeCounters(statsToSend)
        return stats
    }

    private fun collectEventInfo(rtcStats: RTCStatsReport, viewerId: String): Map<String, Any> {
        val relevantStats = rtcStats.statsMap
                .filter { it.value != null }
                .filter { relevantStatsTypes.contains(it.value.type) }
        val eventsToSend = relevantStats
                .map {
                    it.value.members.plus(
                            mapOf(
                                    "id" to it.value.id,
                                    "timestamp" to (it.value.timestampUs / 1000.0),
                                    "type" to it.value.type,
                                    "kind" to it.value.id.substringBefore("_")
                            )
                    )
                }
        val data = mapOf(
                "mode" to "viewer", // TODO: support broadcasts
                "stage_id" to username, // TODO: stage ID
                "viewer_id" to viewerId,
                "stats" to eventsToSend
        )
        return data
    }

}

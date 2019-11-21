package tv.caffeine.app.stage

import android.media.AudioAttributes
import android.media.AudioManager
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsReport
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import timber.log.Timber
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.CumulativeCounters
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.IndividualIceCandidate
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.StatsDimensions
import tv.caffeine.app.api.StatsSnippet
import tv.caffeine.app.api.isOutOfCapacityError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.settings.SettingsStorage
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.webrtc.SimplePeerConnectionObserver
import tv.caffeine.app.webrtc.createAnswer
import tv.caffeine.app.webrtc.getStats
import tv.caffeine.app.webrtc.setLocalDescription
import tv.caffeine.app.webrtc.setRemoteDescription
import java.util.UUID
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

enum class FeedQuality {
    GOOD, POOR, BAD
}

private const val HEARTBEAT_PERIOD_SECONDS = 3L
private const val STATS_REPORTING_PERIOD_SECONDS = 3L

class NewReyesController @AssistedInject constructor(
    private val dispatchConfig: DispatchConfig,
    private val gson: Gson,
    private val realtime: Realtime,
    private val eventsService: EventsService,
    private val peerConnectionFactory: PeerConnectionFactory,
    private val settingsStorage: SettingsStorage,
    private val graphqlStageDirector: GraphqlStageDirector,
    private val audioManager: AudioManager,
    @Assisted private val username: String,
    @Assisted private val muteAudio: Boolean
) : CoroutineScope {

    @AssistedInject.Factory
    interface Factory {
        fun create(username: String, muteAudio: Boolean): NewReyesController
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
    val feedQualityChannel = Channel<FeedQuality>()
    val stateChangeChannel = Channel<List<StateChange>>()
    val errorChannel = Channel<Error>()

    private val peerConnections: MutableMap<String, PeerConnection> = ConcurrentHashMap()
    private val peerConnectionStreamLabels: MutableMap<String, String> = ConcurrentHashMap()
    private val heartbeatUrls: MutableMap<String, String> = ConcurrentHashMap()
    @VisibleForTesting var audioTracks: MutableMap<String, AudioTrack> = ConcurrentHashMap()
    private val feedQualityCounts: MutableMap<String, Int> = ConcurrentHashMap()

    private val videoStreamIds = mutableListOf<String>() // excludes audio-only feeds for connection_quality updates

    fun connect() {
        connectStage()
        heartbeat()
        stats()
    }

    private fun getClientId(): String {
        return settingsStorage.clientId ?: UUID.randomUUID().toString().also { settingsStorage.clientId = it }
    }

    @ExperimentalCoroutinesApi
    private fun connectStage() = launch {
        val stageDirector: StageDirector = graphqlStageDirector

        val uuid = getClientId()
        stageDirector.stageConfiguration(username, uuid).collect { result ->
            if (!isActive) return@collect
            when (result) {
                is CaffeineResult.Success -> onSuccess(result.value)
                is CaffeineResult.Error -> onError(result.error)
                is CaffeineResult.Failure -> onFailure(result.throwable)
            }
        }
    }

    private fun heartbeat() = launch {
        while (isActive) {
            heartbeatUrls.values.forEach { url ->
                val result = realtime.heartbeat(url, Object()).awaitAndParseErrors(gson)
                when (result) {
                    is CaffeineResult.Success -> {
                        val heartbeat = result.value
                        if (heartbeat.id in videoStreamIds) {
                            heartbeat.connectionQuality?.let {
                                processHeartbeatConnectionQuality(heartbeat)
                                feedQualityChannel.send(getFeedQuality())
                            }
                        }
                    }
                    is CaffeineResult.Error -> onError(result.error)
                    is CaffeineResult.Failure -> onFailure(result.throwable)
                }
            }
            delay(TimeUnit.SECONDS.toMillis(HEARTBEAT_PERIOD_SECONDS))
        }
    }

    private fun processHeartbeatConnectionQuality(heartbeat: NewReyes.Heartbeat) {
        val (id, quality) = heartbeat
        if (quality == NewReyes.Quality.GOOD) {
            feedQualityCounts[id] = 0
        } else {
            feedQualityCounts[id]?.let { feedQualityCounts[id] = it + 1 }
        }
    }

    private fun getFeedQuality(): FeedQuality {
        return when (feedQualityCounts.values.max()) {
            0 -> FeedQuality.GOOD
            else -> FeedQuality.POOR
        }
    }

    private fun stats() = launch {
        while (isActive) {
            peerConnections.forEach {
                val streamId = it.key
                val peerConnection = it.value
                peerConnectionStreamLabels[streamId]?.let { streamLabel ->
                    reportStats(peerConnection, streamLabel)
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
            when (it) {
                is StateChange.FeedRemoved -> 1
                is StateChange.FeedAdded -> 2
                is StateChange.FeedRoleChanged -> 3
                is StateChange.FeedStreamChanged -> 4
            }
        }
        return stateChangeList.toList()
    }

    private var feeds: Map<String, NewReyes.Feed> = mapOf()

    private suspend fun onSuccess(newFeeds: Map<String, NewReyes.Feed>) {
        val oldFeeds = feeds
        feedChannel.send(newFeeds)

        newFeeds.values.forEach { feed ->
            audioTracks[feed.stream.id]?.let { audioTrack ->
                Timber.d("DIFF: feed volume changed ${feed.id}, new ${feed.volume}")
                audioTrack.setVolume(feed.volume)
            }
        }
        val diff = diff(oldFeeds, newFeeds)
        diff.forEach {
            when (it) {
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
                    feedQualityCounts.remove(streamId)
                    videoStreamIds.remove(streamId)
                }
        feeds = newFeeds
        diff
            .mapNotNull { stateChange ->
                when (stateChange) {
                    is StateChange.FeedRoleChanged -> stateChange.newFeed
                    else -> null
                }
            }
            .forEach { feed ->
                val streamId = feed.stream.id
                val audioTrack = audioTracks[streamId] ?: return@forEach
                if (feed.capabilities.audio) {
                    audioTrack.setVolume(feed.volume)
                    audioTrack.setEnabled(!muteAudio && requestAudioFocus())
                } else {
                    audioTrack.setEnabled(false)
                }
            }
        diff
                .mapNotNull { stateChange ->
                    when (stateChange) {
                        is StateChange.FeedAdded -> stateChange.feed
                        is StateChange.FeedStreamChanged -> stateChange.feed
                        else -> null
                    }
                }
                .forEach { feed ->
                    val stream = feed.stream
                    if (feed.capabilities.video) {
                        videoStreamIds.add(feed.stream.id)
                    }
                    connectStream(stream)?.let { connectionInfo ->
                        peerConnections[stream.id] = connectionInfo.peerConnection
                        peerConnectionStreamLabels[stream.id] = feed.streamLabel()
                        connectionInfo.audioTrack?.let {
                            audioTracks[stream.id] = it
                            if (feed.capabilities.audio) {
                                it.setVolume(feed.volume)
                                it.setEnabled(!muteAudio && requestAudioFocus())
                            } else {
                                it.setEnabled(false)
                            }
                        }
                        connectionChannel.send(NewReyesFeedInfo(connectionInfo, feed, stream.id, feed.role))
                    }
                }
    }

    @VisibleForTesting val onAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val isAudioFocused = focusChange == AudioManager.AUDIOFOCUS_GAIN
        audioTracks.values.forEach {
            it.setEnabled(!muteAudio && isAudioFocused)
        }
    }

    private fun requestAudioFocus() = audioManager.requestAudioFocus(
        onAudioFocusChangeListener,
        AudioAttributes.CONTENT_TYPE_MOVIE,
        AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

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

    private suspend fun connectStream(stream: NewReyes.Feed.Stream): NewReyesConnectionInfo? {
        val sdpOffer = stream.sdpOffer
        val mediaConstraints = MediaConstraints()
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdpOffer)
        val rtcConfiguration = PeerConnection.RTCConfiguration(listOf())
        val observer = object : SimplePeerConnectionObserver() {
            override fun onIceCandidate(candidate: IceCandidate?) {
                super.onIceCandidate(candidate)
                if (!isActive) {
                    Timber.e("Ice candidate received after closing the stream controller")
                    return
                }
                if (candidate == null) return
                val iceCandidate = IndividualIceCandidate(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)
                val iceCandidates = NewReyes.ConnectToStream(iceCandidates = arrayOf(iceCandidate))
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
        when (result) {
            is CaffeineResult.Success -> {
                Timber.d("Success: ${result.value}")
                val heartbeatUrl = result.value.urls.heartbeat
                heartbeatUrls[stream.id] = heartbeatUrl
                feedQualityCounts[stream.id] = 0
            }
            is CaffeineResult.Error -> {
                Timber.d("Error: ${result.error}")
                return peerConnection.disposeAndReturnNull()
            }
            is CaffeineResult.Failure -> {
                Timber.e(result.throwable)
                return peerConnection.disposeAndReturnNull()
            }
        }
        val result2 = peerConnection.setLocalDescription(localSessionDescription)
        return configureConnections(peerConnection)
    }

    fun close() {
        job.cancel()
        feedChannel.close()
        feedQualityChannel.close()
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
        feedQualityCounts.keys.forEach {
            feedQualityCounts.remove(it)
        }
        audioManager.abandonAudioFocus(onAudioFocusChangeListener)
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

    private val relevantStatsMetrics = listOf("bytesReceived", "packetsReceived", "packetsLost", "framesDecoded")

    private suspend fun reportStats(peerConnection: PeerConnection, streamLabel: String) {
        val rtcStats = peerConnection.getStats()
        sendPerformanceStats(rtcStats, streamLabel)
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
}

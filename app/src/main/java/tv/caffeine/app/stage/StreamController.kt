package tv.caffeine.app.stage

import com.google.gson.Gson
import kotlinx.coroutines.*
import org.webrtc.*
import timber.log.Timber
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.webrtc.createAnswer
import tv.caffeine.app.webrtc.getStats
import tv.caffeine.app.webrtc.setLocalDescription
import tv.caffeine.app.webrtc.setRemoteDescription
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class ConnectionInfo(val peerConnection: PeerConnection, val videoTrack: VideoTrack?, val audioTrack: AudioTrack?)

class StreamController(
        private val dispatchConfig: DispatchConfig,
        private val realtime: Realtime,
        private val peerConnectionFactory: PeerConnectionFactory,
        private val eventsService: EventsService,
        private val gson: Gson,
        private val stageIdentifier: String
) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext get() = dispatchConfig.main + job
    private var closed = false

    suspend fun connect(stream: StageHandshake.Stream) : ConnectionInfo? {
        val result = realtime.createViewer(stream.id).awaitAndParseErrors(gson)
        return when(result) {
            is CaffeineResult.Success -> handleOffer(result.value.offer, result.value.id, result.value.signed_payload, stream.label)
            is CaffeineResult.Error -> Timber.e(Exception("Failed to create viewer for stream ${stream.id}")).run { null }
            is CaffeineResult.Failure -> Timber.e(result.throwable).run { null }
        }
    }

    fun close() {
        job.cancel()
        closed = true
    }

    private suspend fun handleOffer(offer: String, viewerId: String, signedPayload: String, streamLabel: String): ConnectionInfo? {
        val mediaConstraints = MediaConstraints()
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offer)
        val rtcConfiguration = PeerConnection.RTCConfiguration(listOf())
        val observer = object : SimplePeerConnectionObserver() {
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                super.onIceConnectionChange(newState)
                Timber.d("onIceConnectionChange: $newState")
                if (newState == null) return
                val data = mapOf(
                        "connection_state" to newState.name,
                        "stage_id" to stageIdentifier,
                        "mode" to "viewer", // TODO: support broadcast
                        "viewer_id" to viewerId
                )
                launch {
                    val result = eventsService.sendEvent(EventBody("ice_connection_state", data = data)).awaitAndParseErrors(gson)
                    when (result) {
                        is CaffeineResult.Success -> Timber.d("ICE connection state event sent, ${result.value}")
                        is CaffeineResult.Error -> Timber.e(Exception("Failed to send ICE connection state event"))
                        is CaffeineResult.Failure -> Timber.e(result.throwable)
                    }
                }
            }

            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                if (closed) {
                    Timber.e(Exception("ICE"), "Ice candidate received after closing the stream controller")
                    return
                }
                if (iceCandidate == null) return
                val candidate = IndividualIceCandidate(iceCandidate.sdp, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex)
                val body = IceCandidatesBody(arrayOf(candidate), signedPayload)
                launch {
                    val result = realtime.sendIceCandidate(viewerId, body).awaitAndParseErrors(gson)
                    when (result) {
                        is CaffeineResult.Success -> Timber.d("ICE candidate sent, ${result.value}")
                        is CaffeineResult.Error -> Timber.e(Exception("Failed to send ICE candidate"))
                        is CaffeineResult.Failure -> Timber.e(result.throwable)
                    }
                }
            }
        }
        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, observer) ?: return null
        val result1 = peerConnection.setRemoteDescription(sessionDescription)
        val localSessionDescription = peerConnection.createAnswer(mediaConstraints) ?: return peerConnection.disposeAndReturnNull()
        val result2 = peerConnection.setLocalDescription(localSessionDescription)
        Timber.d("LocalDesc: Success! $localSessionDescription - ${localSessionDescription.type} - ${localSessionDescription.description}")
        val answer = localSessionDescription.description
        val result = realtime.sendAnswer(viewerId, AnswerBody(answer, signedPayload)).awaitAndParseErrors(gson)
        return when (result) {
            is CaffeineResult.Success -> configureConnections(peerConnection, viewerId, signedPayload, streamLabel)
            is CaffeineResult.Error -> Timber.e(Exception("sendAnswer failed")).run { peerConnection.disposeAndReturnNull() }
            is CaffeineResult.Failure -> Timber.e(result.throwable).run { peerConnection.disposeAndReturnNull() }
        }
    }

    private fun PeerConnection.disposeAndReturnNull(): ConnectionInfo? {
        dispose()
        return null
    }

    private suspend fun configureConnections(peerConnection: PeerConnection, viewerId: String, signedPayload: String, streamLabel: String): ConnectionInfo {
        val receivers = peerConnection.receivers
        val videoTrack = receivers.find { it.track() is VideoTrack }?.track() as? VideoTrack
        val audioTrack = receivers.find { it.track() is AudioTrack }?.track() as? AudioTrack
        val connectionInfo = ConnectionInfo(peerConnection, videoTrack, audioTrack)
        launch {
            while (isActive) {
                repeat(5) {
                    if (closed) return@launch
                    reportStats(peerConnection, viewerId, streamLabel)
                    delay(TimeUnit.SECONDS.toMillis(3))
                }
                sendHeartbeat(signedPayload, viewerId)
            }
        }
        return connectionInfo
    }

    private val relevantStatsTypes = listOf("inbound-rtp", "candidate-pair", "remote-candidate", "local-candidate", "track")
    private val relevantStatsMetrics = listOf("bytesReceived", "packetsReceived", "packetsLost", "framesDecoded")
    private val streamLabelToSourceName = mapOf("game" to "content", "webcam" to "camera")
    private val defaultSourceName = "content"

    private suspend fun reportStats(peerConnection: PeerConnection, viewerId: String, streamLabel: String) {
        val rtcStats = peerConnection.getStats()
        val data = collectEventInfo(rtcStats, viewerId)
        val sendEventResult = eventsService.sendEvent(EventBody("webrtc_stats", data = data)).awaitAndParseErrors(gson)
        when (sendEventResult) {
            is CaffeineResult.Success -> Timber.d("Successfully sent event")
            is CaffeineResult.Error -> Timber.e(Exception("Error sending event webrtc_stats"))
            is CaffeineResult.Failure -> Timber.e(sendEventResult.throwable)
        }
        val stats = collectPerformanceStats(rtcStats, streamLabel)
        val sendStatsResult = eventsService.sendStats(stats).awaitAndParseErrors(gson)
        when (sendStatsResult) {
            is CaffeineResult.Success -> Timber.d("Successfully sent stats")
            is CaffeineResult.Error -> Timber.e(Exception("Error sending stats webrtc_stats"))
            is CaffeineResult.Failure -> Timber.e(sendStatsResult.throwable)
        }
    }

    private fun collectPerformanceStats(rtcStats: RTCStatsReport, streamLabel: String): CumulativeCounters {
        val statsToSend: List<StatsSnippet> = rtcStats.statsMap
                .filter { it.value.type == "inbound-rtp" }
                .map { stat ->
                    val mediaType = stat.value.members["mediaType"] as String
                    val sourceName = streamLabelToSourceName[streamLabel] ?: defaultSourceName
                    Timber.d("Stats available: ${stat.value.members.keys}")
                    return@map relevantStatsMetrics
                            .filter { stat.value.members.containsKey(it) }
                            .mapNotNull { metricName ->
                                Timber.d("Stats available: $metricName = ${stat.value.members[metricName]}")
                                stat.value.members[metricName]?.toString()?.toInt()?.let { metricValue ->
                                    StatsSnippet(StatsDimensions(mediaType, sourceName, stageIdentifier), "rtc.$metricName", metricValue)
                                }
                            }
                }
                .flatten()
        val stats = CumulativeCounters(statsToSend)
        return stats
    }

    private fun collectEventInfo(rtcStats: RTCStatsReport, viewerId: String): Map<String, Any> {
        val relevantStats = rtcStats.statsMap
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
                "stage_id" to stageIdentifier,
                "viewer_id" to viewerId,
                "stats" to eventsToSend
        )
        return data
    }

    private suspend fun sendHeartbeat(signedPayload: String, viewerId: String) {
        val heartbeatBody = HeartbeatBody(signedPayload)
        val sendHeartbeatResult = realtime.sendHeartbeat(viewerId, heartbeatBody).awaitAndParseErrors(gson)
        when (sendHeartbeatResult) {
            is CaffeineResult.Success -> Timber.d("Successfully sent heartbeat")
            is CaffeineResult.Error -> Timber.e(Exception("Error sending heartbeat"))
            is CaffeineResult.Failure -> Timber.e(sendHeartbeatResult.throwable)
        }
    }
}

open class SimplePeerConnectionObserver : PeerConnection.Observer {
    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Timber.d("onSignalingChange: $newState")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Timber.d("onIceConnectionChange: $newState")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Timber.d("onIceConnectionReceivingChange: $receiving")
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        Timber.d("onIceGatheringChange: $newState")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Timber.d("onIceCandidate: $candidate")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        Timber.d("onIceCandidatesRemoved: $candidates")
    }

    override fun onAddStream(stream: MediaStream?) {
        Timber.d("onAddStream: $stream")
    }

    override fun onRemoveStream(stream: MediaStream?) {
        Timber.d("onRemoveStream: $stream")
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
        Timber.d("onDataChannel: $dataChannel")
    }

    override fun onRenegotiationNeeded() {
        Timber.d("onRenegotiationNeeded")
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        Timber.d("onAddTrack: $receiver, $mediaStreams")
    }

}

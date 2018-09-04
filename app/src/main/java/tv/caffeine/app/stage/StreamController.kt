package tv.caffeine.app.stage

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.webrtc.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.*

class StreamController(private val realtime: Realtime,
                       private val peerConnectionFactory: PeerConnectionFactory,
                       private val eventsService: EventsService,
                       private val stageIdentifier: String) {
    private var heartbeatJob: Job? = null

    fun connect(stream: StageHandshake.Stream, callback: (PeerConnection, VideoTrack?, AudioTrack?) -> Unit) {
        realtime.createViewer(stream.id).enqueue(object: Callback<CreateViewerResult?> {
            override fun onFailure(call: Call<CreateViewerResult?>?, t: Throwable?) {
                Timber.e(t, "Failed to create viewer for stream ID: ${stream.id}")
            }

            override fun onResponse(call: Call<CreateViewerResult?>?, response: retrofit2.Response<CreateViewerResult?>?) {
                Timber.d("Created viewer for stream ID ${stream.id}, ${response?.body()}, ${call?.request()?.headers()}, ${response?.headers()}")
                response?.body()?.let {
                    Timber.d("Got offer ${it.offer}")
                    handleOffer(it.offer, it.id, it.signed_payload, callback)
                }
            }
        })
    }

    fun close() {
        heartbeatJob?.cancel()
    }

    fun handleOffer(offer: String, viewerId: String, signedPayload: String, callback: (PeerConnection, VideoTrack?, AudioTrack?) -> Unit) {
        val mediaConstraints = MediaConstraints()
        val sanitizedOffer = offer.replace("42001f", "42e01f")
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sanitizedOffer)
        val rtcConfiguration = PeerConnection.RTCConfiguration(listOf())
        val observer = object : PeerConnectionObserver(eventsService, viewerId, stageIdentifier) {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                if (iceCandidate == null) return
                val candidate = IndividualIceCandidate(iceCandidate.sdp, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex)
                val body = IceCandidatesBody(arrayOf(candidate), signedPayload)
                realtime.sendIceCandidate(viewerId, body).enqueue(object: Callback<Void?> {
                    override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                        Timber.e(t, "Failed to send ICE candidates")
                    }

                    override fun onResponse(call: Call<Void?>?, response: retrofit2.Response<Void?>?) {
                        Timber.d("ICE candidates sent, $response")
                    }
                })
            }
        }
        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, observer) ?: return
        peerConnection.setRemoteDescription(object : CafSdpObserver() {
            override fun onSetSuccess() {
                super.onSetSuccess()
                peerConnection.createAnswer(object: CafSdpObserver() {
                    override fun onCreateSuccess(localSessionDescription: SessionDescription?) {
                        super.onCreateSuccess(localSessionDescription)
                        peerConnection.setLocalDescription(object: CafSdpObserver() {
                            override fun onSetSuccess() {
                                super.onSetSuccess()
                                //
                                Timber.d("LocalDesc: Success! $localSessionDescription - ${localSessionDescription?.type} - ${localSessionDescription?.description}")
                                localSessionDescription?.let {
                                    val answer = it.description
                                    realtime.sendAnswer(viewerId, AnswerBody(answer, signedPayload)).enqueue(object: Callback<Void?> {
                                        override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                                            Timber.e(t, "sendAnswer failed")
                                        }

                                        override fun onResponse(call: Call<Void?>?, response: retrofit2.Response<Void?>?) {
                                            Timber.d("sendAnswer succeeded $response, ${response?.body()}")
                                            val receivers = peerConnection.receivers
                                            val videoTrack = receivers.find { it.track() is VideoTrack }?.track() as? VideoTrack
                                            val audioTrack = receivers.find { it.track() is AudioTrack }?.track() as? AudioTrack
                                            callback(peerConnection, videoTrack, audioTrack)
                                            heartbeatJob = launch {
                                                val relevantStats = listOf("inbound-rtp", "candidate-pair", "remote-candidate", "local-candidate", "track")
                                                while(true) {
                                                    repeat(5) {
                                                        peerConnection.getStats { stats ->
                                                            val relevantStats = stats.statsMap
                                                                    .filter { relevantStats.contains(it.value.type) }
                                                                    .map {
                                                                        it.value.members.plus(
                                                                                mapOf(
                                                                                        "id" to it.value.id,
                                                                                        "timestamp" to (it.value.timestampUs / 1000.0).toInt().toString(),
                                                                                        "type" to it.value.type,
                                                                                        "kind" to it.value.id.substringBefore("_")
                                                                                )
                                                                        )
                                                                    }
                                                            val data = mapOf(
                                                                    "mode" to "viewer", // TODO: support broadcasts
                                                                    "stats" to relevantStats
                                                            )
                                                            eventsService.sendEvent(EventBody("webrtc_stats", data = data)).enqueue(object: Callback<Void?> {
                                                                override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                                                                    Timber.e(t, "Failed to send the event")
                                                                }

                                                                override fun onResponse(call: Call<Void?>?, response: Response<Void?>?) {
                                                                    Timber.d("Sent the event, got response $response")
                                                                    response?.body()?.let { body ->
                                                                        Timber.d("Got response body: $body")
                                                                    }
                                                                }
                                                            })
                                                        }
                                                        delay(3, java.util.concurrent.TimeUnit.SECONDS)
                                                    }
                                                    val heartbeatBody = HeartbeatBody(signedPayload)
                                                    realtime.sendHeartbeat(viewerId, heartbeatBody).enqueue(object: Callback<Void?> {
                                                        override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                                                            Timber.e(t, "Failed to send a heartbeat")
                                                        }

                                                        override fun onResponse(call: Call<Void?>?, response: Response<Void?>?) {
                                                            Timber.d("Sent heartbeat, got response $response")
                                                        }
                                                    })
                                                }
                                            }
                                        }
                                    })
                                }

                            }
                        }, localSessionDescription)
                    }
                }, mediaConstraints)
            }
        }, sessionDescription)

    }
}

open class PeerConnectionObserver(private val eventsService: EventsService,
                                  private val viewerId: String,
                                  private val stageIdentifier: String) : PeerConnection.Observer {
    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Timber.d("onSignalingChange: $newState")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Timber.d("onIceConnectionChange: $newState")
        if (newState == null) return
        val data = mapOf(
                "connection_state" to newState.name,
                "stage_id" to stageIdentifier,
                "mode" to "viewer", // TODO: support broadcast
                "viewer_id" to viewerId
                )
        eventsService.sendEvent(EventBody("ice_connection_state", data = data))
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

open class CafSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {
        Timber.d("CafSdpObserver Create success $sdp!")
    }

    override fun onSetSuccess() {
        Timber.d("CafSdpObserver Set success!")
    }

    override fun onCreateFailure(error: String?) {
        Timber.e("CafSdpObserver Create failure, error: $error")
    }

    override fun onSetFailure(error: String?) {
        Timber.e("CafSdpObserver Set failure, error: $error")
    }

}

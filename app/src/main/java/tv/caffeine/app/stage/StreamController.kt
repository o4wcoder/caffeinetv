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
    private val heartbeatJobs: MutableList<Job> = mutableListOf()
    private var closed = false

    fun connect(stream: StageHandshake.Stream, callback: (PeerConnection, VideoTrack?, AudioTrack?) -> Unit) {
        realtime.createViewer(stream.id).enqueue(object : Callback<CreateViewerResult?> {
            override fun onFailure(call: Call<CreateViewerResult?>?, t: Throwable?) {
                Timber.e(t, "Failed to create viewer for stream ID: ${stream.id}")
            }

            override fun onResponse(call: Call<CreateViewerResult?>?, response: retrofit2.Response<CreateViewerResult?>?) {
                Timber.d("Created viewer for stream ID ${stream.id}, ${response?.body()}, ${call?.request()?.headers()}, ${response?.headers()}")
                if (closed) {
                    Timber.e(Exception("VIEW"), "Viewer created after closing the stream controller")
                    return
                }
                response?.body()?.let {
                    Timber.d("Got offer ${it.offer}")
                    handleOffer(it.offer, it.id, it.signed_payload, callback)
                }
            }
        })
    }

    fun close() {
        closed = true
        Timber.d("Canceling ${heartbeatJobs.count()} heartbeat jobs")
        heartbeatJobs.forEach { it.cancel() }
    }

    fun handleOffer(offer: String, viewerId: String, signedPayload: String, callback: (PeerConnection, VideoTrack?, AudioTrack?) -> Unit) {
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
                eventsService.sendEvent(EventBody("ice_connection_state", data = data))
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
                realtime.sendIceCandidate(viewerId, body).enqueue(object : Callback<Void?> {
                    override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                        Timber.e(t, "Failed to send ICE candidates")
                    }

                    override fun onResponse(call: Call<Void?>?, response: retrofit2.Response<Void?>?) {
                        Timber.d("ICE candidates sent, $response")
                    }
                })
            }
        }
        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, observer)
                ?: return
        peerConnection.setRemoteDescription(sessionDescription) {
            peerConnection.createAnswer(mediaConstraints) { localSessionDescription ->
                if (localSessionDescription == null) return@createAnswer
                peerConnection.setLocalDescription(localSessionDescription) {
                    Timber.d("LocalDesc: Success! $localSessionDescription - ${localSessionDescription.type} - ${localSessionDescription.description}")
                    val answer = localSessionDescription.description
                    realtime.sendAnswer(viewerId, AnswerBody(answer, signedPayload)).enqueue(object : Callback<Void?> {
                        override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                            Timber.e(t, "sendAnswer failed")
                        }

                        override fun onResponse(call: Call<Void?>?, response: retrofit2.Response<Void?>?) {
                            Timber.d("sendAnswer succeeded $response, ${response?.body()}")
                            val receivers = peerConnection.receivers
                            val videoTrack = receivers.find { it.track() is VideoTrack }?.track() as? VideoTrack
                            val audioTrack = receivers.find { it.track() is AudioTrack }?.track() as? AudioTrack
                            callback(peerConnection, videoTrack, audioTrack)
                            heartbeatJobs.add(launch {
                                val relevantStats = listOf("inbound-rtp", "candidate-pair", "remote-candidate", "local-candidate", "track")
                                while (isActive) {
                                    repeat(5) {
                                        if (closed) return@launch
                                        peerConnection.getStats { stats ->
                                            val statsToSend = stats.statsMap
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
                                                    "stats" to statsToSend
                                            )
                                            eventsService.sendEvent(EventBody("webrtc_stats", data = data)).enqueue(object : Callback<Void?> {
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
                                    realtime.sendHeartbeat(viewerId, heartbeatBody).enqueue(object : Callback<Void?> {
                                        override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                                            Timber.e(t, "Failed to send a heartbeat")
                                        }

                                        override fun onResponse(call: Call<Void?>?, response: Response<Void?>?) {
                                            Timber.d("Sent heartbeat, got response $response")
                                        }
                                    })
                                }
                            })
                        }
                    })
                }
            }
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

inline fun PeerConnection.setRemoteDescription(sessionDescription: SessionDescription, crossinline callback: () -> Unit) {
    setRemoteDescription(object : CafSdpObserver() {
        override fun onSetSuccess() {
            super.onSetSuccess()
            callback()
        }
    }, sessionDescription)
}

inline fun PeerConnection.createAnswer(mediaConstraints: MediaConstraints, crossinline callback: (localSessionDescription: SessionDescription?) -> Unit) {
    createAnswer(object : CafSdpObserver() {
        override fun onCreateSuccess(localSessionDescription: SessionDescription?) {
            super.onCreateSuccess(localSessionDescription)
            callback(localSessionDescription)
        }
    }, mediaConstraints)
}

inline fun PeerConnection.setLocalDescription(sessionDescription: SessionDescription, crossinline callback: () -> Unit) {
    setLocalDescription(object : CafSdpObserver() {
        override fun onSetSuccess() {
            super.onSetSuccess()
            callback()
        }
    }, sessionDescription)
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

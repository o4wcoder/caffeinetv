package tv.caffeine.app.stage

import org.webrtc.*
import retrofit2.Call
import retrofit2.Callback
import timber.log.Timber
import tv.caffeine.app.realtime.*

class StreamController(val realtime: Realtime, val accessToken: String, val xCredential: String, val peerConnectionFactory: PeerConnectionFactory) {
    fun connect(stream: StageHandshake.Stream, callback: (PeerConnection, VideoTrack?, AudioTrack?) -> Unit) {
        realtime.createViewer("Bearer $accessToken", xCredential, stream.id).enqueue(object: Callback<CreateViewerResult?> {
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

    fun handleOffer(offer: String, viewerId: String, signedPayload: String, callback: (PeerConnection, VideoTrack?, AudioTrack?) -> Unit) {
        val mediaConstraints = MediaConstraints()
        val sanitizedOffer = offer.replace("42001f", "42e01f")
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sanitizedOffer)
        val rtcConfiguration = PeerConnection.RTCConfiguration(listOf())
        val observer = object : PeerConnectionObserver() {
            var peerConnection: PeerConnection? = null
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                if (iceCandidate == null) return
                peerConnection?.addIceCandidate(iceCandidate)
                val candidate = IndividualIceCandidate(iceCandidate.sdp, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex)
                val body = IceCandidatesBody(arrayOf(candidate), signedPayload)
                realtime.sendIceCandidate("Bearer $accessToken", xCredential, body, viewerId).enqueue(object: Callback<Void?> {
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
        observer.peerConnection = peerConnection
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
                                    realtime.sendAnswer("Bearer $accessToken", xCredential, AnswerBody(answer, signedPayload), viewerId).enqueue(object: Callback<Void?> {
                                        override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                                            Timber.e(t, "sendAnswer failed")
                                        }

                                        override fun onResponse(call: Call<Void?>?, response: retrofit2.Response<Void?>?) {
                                            Timber.d("sendAnswer succeeded $response, ${response?.body()}")
                                            val receivers = peerConnection.receivers
                                            val videoTrack = receivers.find { it.track() is VideoTrack }?.track() as? VideoTrack
                                            val audioTrack = receivers.find { it.track() is AudioTrack }?.track() as? AudioTrack
                                            callback(peerConnection, videoTrack, audioTrack)
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

open class PeerConnectionObserver : PeerConnection.Observer {
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

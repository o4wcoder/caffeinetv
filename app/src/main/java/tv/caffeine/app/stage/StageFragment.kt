package tv.caffeine.app.stage


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_stage.*
import okhttp3.*
import org.webrtc.*
import retrofit2.Call
import retrofit2.Callback
import timber.log.Timber
import tv.caffeine.app.R
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation

class StageFragment : DaggerFragment() {
    var accessToken: String? = null
    var xCredential: String? = null
    var stageIdentifier : String? = null
    var broadcaster: String? = null
    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accessToken = arguments?.getString("ACCESS_TOKEN")
        xCredential = arguments?.getString("X_CREDENTIAL")
        stageIdentifier = arguments?.getString("STAGE_IDENTIFIER")
        broadcaster = arguments?.getString("BROADCASTER")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url("wss://realtime.caffeine.tv/v2/stages/${stageIdentifier!!}/details").build()
        val listener = StageSocketListener(accessToken!!, xCredential!!, activity!!, surface_view_renderer, realtime, peerConnectionFactory)
        okHttpClient.newWebSocket(request, listener)

        surface_view_renderer.init(eglBase.eglBaseContext, null)
        surface_view_renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        surface_view_renderer.setEnableHardwareScaler(true)
//        surface_view_renderer.setZOrderMediaOverlay(true)
//        surface_view_renderer.setZOrderOnTop(true)

        open_in_browser.setOnClickListener { openInBrowser() }
    }

    private fun openInBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.caffeine.tv/$broadcaster"))
        startActivity(intent)
    }

}


//const val NORMAL_CLOSE_CODE = 100

class StageSocketListener(val accessToken: String, val xCredential: String, val context: Context, val surface_view_renderer: SurfaceViewRenderer, val realtime: Realtime, val peerConnectionFactory: PeerConnectionFactory) : WebSocketListener() {
    val gsonForHandshake: Gson
    val gsonForEvents: Gson
    var messageNumber: Int = 0
    init {
        gsonForHandshake = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        gsonForEvents = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    }
    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        Timber.d("Opened, response = $response")
        webSocket?.send("""{
                "Headers": {
                    "x-credential" : "$xCredential",
                    "authorization" : "Bearer $accessToken",
                    "X-Client-Type" : "android",
                    "X-Client-Version" : "0"
                }
            }""".trimMargin())
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        Timber.d("Got message $text")
        if (messageNumber == 0) {
            text?.let {
                val handshake = gsonForHandshake.fromJson(it, WebSocketHandshake::class.java)
                handshake
            }
        } else if (messageNumber == 1) {
            text?.let {
                val eventEnvelope = gsonForEvents.fromJson(it, WebSocketEventEnvelope::class.java)
                eventEnvelope.v2.streams.firstOrNull { it.type == "primary" }?.let { stream ->
                    realtime.createViewer("Bearer $accessToken", xCredential, stream.id).enqueue(object: Callback<CreateViewerResult?> {
                        override fun onFailure(call: Call<CreateViewerResult?>?, t: Throwable?) {
                            Timber.e(t, "Failed to create viewer for stream ID: ${stream.id}")
                        }

                        override fun onResponse(call: Call<CreateViewerResult?>?, response: retrofit2.Response<CreateViewerResult?>?) {
                            Timber.d("Created viewer for stream ID ${stream.id}, ${response?.body()}, ${call?.request()?.headers()}, ${response?.headers()}")
                            response?.body()?.let {
                                Timber.d("Got offer ${it.offer}")
                                handleOffer(it.offer, context, surface_view_renderer, realtime, accessToken, xCredential, stream.id, it.id, it.signed_payload, peerConnectionFactory)
                            }
                        }
                    })
                }
            }
        }
        messageNumber++

    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        Timber.d("Closing, code = $code, reason = $reason")
    }
}

class WebSocketHandshake(val body: String, val compatibilityMode: Boolean, val headers: Map<String, String>, val status: Int)
class WebSocketEventEnvelope(val v2: WebSocketEvent)
class WebSocketEvent(val gameId: String, val hostConnectionQuality: String, val sessionId: String, val state: String, val streams: Array<WebSocketStream>, val title: String)
class WebSocketStream(val capabilities: Map<String, Boolean>, val id: String, val label: String, val type: String)

fun handleOffer(offer: String, context: Context, surface_view_renderer: SurfaceViewRenderer, realtime: Realtime, accessToken: String, xCredential: String, streamId: String, viewerId: String, signedPayload: String, peerConnectionFactory: PeerConnectionFactory) {
    handleInitStream(offer, context, surface_view_renderer, realtime, accessToken, xCredential, streamId, viewerId, signedPayload, peerConnectionFactory)
}


fun handleOfferAlt(offer: String, context: Context, surface_view_renderer: SurfaceViewRenderer, realtime: Realtime, accessToken: String, xCredential: String, streamId: String, viewerId: String, signedPayload: String, peerConnectionFactory: PeerConnectionFactory) {
    handleInitStream(offer, context, surface_view_renderer, realtime, accessToken, xCredential, streamId, viewerId, signedPayload, peerConnectionFactory)
    val initBody = StreamInitBody(streamId, offer)
    realtime.initializeStream("Bearer $accessToken", xCredential, initBody).enqueue(object : Callback<StreamInitResult?> {
        override fun onFailure(call: Call<StreamInitResult?>?, t: Throwable?) {
        }

        override fun onResponse(call: Call<StreamInitResult?>?, response: retrofit2.Response<StreamInitResult?>?) {
            val streamInitResult = response?.body() ?: return
            handleInitStream(offer, context, surface_view_renderer, realtime, accessToken, xCredential, streamId, viewerId, streamInitResult.signed_payload, peerConnectionFactory)
        }
    })
}

fun handleInitStream(offer: String, context: Context, surface_view_renderer: SurfaceViewRenderer, realtime: Realtime, accessToken: String, xCredential: String, streamId: String, viewerId: String, signedPayload: String, peerConnectionFactory: PeerConnectionFactory) {
    val mediaConstraints = MediaConstraints()
    val sanitizedOffer = offer.replace("42001f", "42e01f")
    val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sanitizedOffer)
    val rtcConfiguration = PeerConnection.RTCConfiguration(listOf())
    val observer = object : PeerConnectionObserver() {
        var peerConnection: PeerConnection? = null
        override fun onIceCandidate(candidate: IceCandidate?) {
            super.onIceCandidate(candidate)
            if (candidate == null) return
            peerConnection?.addIceCandidate(candidate)
            val candidate = IndividualIceCandidate(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)
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
                                        val numberOfVideoTracks = receivers.count { it.track() is VideoTrack }
                                        val numberOfAudioTracks = receivers.count { it.track() is AudioTrack }
                                        Timber.d("Found $numberOfVideoTracks video tracks and $numberOfAudioTracks audio tracks")
                                        val videoTrack = receivers
                                                .find { it.track() is VideoTrack }
                                                ?.track() as? VideoTrack
                                        Timber.d("Track: $videoTrack")
                                        videoTrack?.apply {
                                            videoTrack.addSink(surface_view_renderer)
                                            Timber.d("Track id = ${videoTrack.id()}, kind = ${videoTrack.kind()}, state = ${videoTrack.state().name}")
                                        }
                                        val audioTrack = receivers
                                                .find { it.track() is AudioTrack }
                                                ?.track() as? AudioTrack
                                        audioTrack?.setVolume(0.5) //TODO: make it possible to control volume
                                        Timber.d("ICE Connection State: ${peerConnection.iceConnectionState()}")
                                        // Below fails with "GetTransceivers is only supported with Unified Plan SdpSemantics."
//                            peerConnection.transceivers
//                                    .find { it.receiver.track() is VideoTrack }
//                                    ?.receiver?.track()?.apply { (this as VideoTrack).addSink(surface_view_renderer) }
                                    }
                                })
                            }

                        }
                    }, localSessionDescription)
                }
            }, mediaConstraints)
        }
    }, sessionDescription)

    /*
    launch(CommonPool) {
        try {
            peerConnection.setRemoteDescription(sessionDescription)
            val localSessionDescription = peerConnection.createAnswer(mediaConstraints)
            peerConnection.setLocalDescription(localSessionDescription)
        } catch (e: Exception) {
            Timber.e(e, "Coroutines: Error")
        }
    }
    */
}

/*
suspend fun PeerConnection.setRemoteDescription(sessionDescription: SessionDescription) {
    return suspendCoroutine { cont ->
        val coroutineSetSdpObserver = CoroutineSetSdpObserver(cont)
        setRemoteDescription(coroutineSetSdpObserver, sessionDescription)
    }
}

suspend fun PeerConnection.createAnswer(mediaConstraints: MediaConstraints): SessionDescription {
    return suspendCoroutine { cont ->
        val coroutineCreateSdpObserver = CoroutineCreateSdpObserver(cont)
        createAnswer(coroutineCreateSdpObserver, mediaConstraints)
    }
}

suspend fun PeerConnection.setLocalDescription(sessionDescription: SessionDescription) {
    return suspendCoroutine { cont ->
        val coroutineSetSdpObserver = CoroutineSetSdpObserver(cont)
        setLocalDescription(coroutineSetSdpObserver, sessionDescription)
    }
}
*/

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

class CoroutineCreateSdpObserver(private val continuation: Continuation<SessionDescription>) : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        Timber.d("CoroutineCreateSdpObserver onCreateSuccess")
        if (sessionDescription == null) return continuation.resumeWithException(Exception("Missing session description"))
        continuation.resume(sessionDescription)
    }

    override fun onSetSuccess() {
        Timber.d("CoroutineCreateSdpObserver onSetSuccess")
        continuation.resumeWithException(Exception("Not expected to use set success"))
    }

    override fun onCreateFailure(p0: String?) {
        Timber.d("CoroutineCreateSdpObserver onCreateFailure")
        continuation.resumeWithException(Exception(p0))
    }

    override fun onSetFailure(p0: String?) {
        Timber.d("CoroutineCreateSdpObserver onSetFailure")
        continuation.resumeWithException(Exception("Not expected to use set failure"))
    }
}

class CoroutineSetSdpObserver(private val continuation: Continuation<Unit>) : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        Timber.d("CoroutineSetSdpObserver onCreateSuccess")
        continuation.resumeWithException(Exception("Not expected to use create success"))
    }

    override fun onSetSuccess() {
        Timber.d("CoroutineSetSdpObserver onSetSuccess")
        continuation.resume(Unit)
    }

    override fun onCreateFailure(p0: String?) {
        Timber.d("CoroutineSetSdpObserver onCreateFailure")
        continuation.resumeWithException(Exception("Not expected to use create failure"))
    }

    override fun onSetFailure(p0: String?) {
        Timber.d("CoroutineSetSdpObserver onSetFailure")
        continuation.resumeWithException(Exception(p0))
    }
}

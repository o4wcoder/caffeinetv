package tv.caffeine.app.stage


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

class StageFragment : DaggerFragment() {
    lateinit var accessToken: String
    lateinit var xCredential: String
    lateinit var stageIdentifier : String
    lateinit var broadcaster: String
    var peerConnection: PeerConnection? = null

    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.run {
            accessToken = getString("ACCESS_TOKEN")!!
            xCredential = getString("X_CREDENTIAL")!!
            stageIdentifier = getString("STAGE_IDENTIFIER")!!
            broadcaster = getString("BROADCASTER")!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectStreams()

        configureSurfaceViewRenderer()

        open_in_browser.setOnClickListener { openInBrowser() }
    }

    private fun configureSurfaceViewRenderer() {
        surface_view_renderer.init(eglBase.eglBaseContext, null)
        surface_view_renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        surface_view_renderer.setEnableHardwareScaler(true)
    }

    private fun connectStreams() {
        val stageHandshake = StageHandshake(accessToken, xCredential)
        val streamController = StreamController(realtime, accessToken, xCredential, peerConnectionFactory)
        stageHandshake.connect(stageIdentifier) { event ->
            val stream = event.streams.find { it.type == "primary" } ?: return@connect
            streamController.connect(stream) { peerConnection ->
                this.peerConnection = peerConnection
                val receivers = peerConnection.receivers
                val videoTrack = receivers.find { it.track() is VideoTrack }?.track() as? VideoTrack
                videoTrack?.run { addSink(surface_view_renderer) }
                val audioTrack = receivers.find { it.track() is AudioTrack }?.track() as? AudioTrack
                audioTrack?.setVolume(0.5) //TODO: make it possible to control volume
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        surface_view_renderer.release()
        peerConnection?.dispose()
    }

    private fun openInBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.caffeine.tv/$broadcaster"))
        startActivity(intent)
    }

}

class StageHandshake(private val accessToken: String, private val xCredential: String) {
    fun connect(stageIdentifier: String, callback: (WebSocketEvent) -> Unit) {
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url("wss://realtime.caffeine.tv/v2/stages/$stageIdentifier/details").build()
        val headers = """{
                "Headers": {
                    "x-credential" : "$xCredential",
                    "authorization" : "Bearer $accessToken",
                    "X-Client-Type" : "android",
                    "X-Client-Version" : "0"
                }
            }""".trimMargin()
        val listener = StageHandshakeListener(headers, callback)
        okHttpClient.newWebSocket(request, listener)
    }
}

class StageHandshakeListener(private val headers: String, val callback: (WebSocketEvent) -> Unit): WebSocketListener() {
    val gsonForHandshake: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    val gsonForEvents: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    var messageNumber: Int = 0

    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        Timber.d("Opened, response = $response")
        webSocket?.send(headers)
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        Timber.d("Got message $text")
        when (messageNumber++) {
            0 -> text?.let { gsonForHandshake.fromJson(it, WebSocketHandshake::class.java) }
            1 -> text?.let {
                val eventEnvelope = gsonForEvents.fromJson(it, WebSocketEventEnvelope::class.java)
                callback(eventEnvelope.v2)
            }
        }
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        Timber.d("Closing, code = $code, reason = $reason")
    }

}

class StreamController(val realtime: Realtime, val accessToken: String, val xCredential: String, val peerConnectionFactory: PeerConnectionFactory) {
    fun connect(stream: WebSocketStream, callback: (PeerConnection) -> Unit) {
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

    fun handleOffer(offer: String, viewerId: String, signedPayload: String, callback: (PeerConnection) -> Unit) {
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
                                            callback(peerConnection)
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

class WebSocketHandshake(val body: String, val compatibilityMode: Boolean, val headers: Map<String, String>, val status: Int)
class WebSocketEventEnvelope(val v2: WebSocketEvent)
class WebSocketEvent(val gameId: String, val hostConnectionQuality: String, val sessionId: String, val state: String, val streams: Array<WebSocketStream>, val title: String)
class WebSocketStream(val capabilities: Map<String, Boolean>, val id: String, val label: String, val type: String)

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

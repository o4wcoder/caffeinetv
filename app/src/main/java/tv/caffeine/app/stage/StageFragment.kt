package tv.caffeine.app.stage


import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_stage.*
import org.webrtc.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.realtime.Realtime
import javax.inject.Inject

class StageFragment : DaggerFragment() {
    lateinit var stageIdentifier : String
    lateinit var broadcaster: String
    private val peerConnections: MutableMap<String, PeerConnection> = mutableMapOf()
    private val renderers: MutableMap<String, SurfaceViewRenderer> = mutableMapOf()
    var stageHandshake: StageHandshake? = null
    var messageHandshake: MessageHandshake? = null
    var streamController: StreamController? = null

    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.run {
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
    }

    private fun configureSurfaceViewRenderer() {
        renderers["primary"] = primary_view_renderer
        renderers["secondary"] = secondary_view_renderer
        listOf(primary_view_renderer, secondary_view_renderer)
                .forEach {
                    it.init(eglBase.eglBaseContext, null)
                    it.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    it.setEnableHardwareScaler(true)
                }
    }

    private fun connectStreams() {
        activity?.volumeControlStream = AudioManager.STREAM_VOICE_CALL
        stageHandshake = StageHandshake(tokenStore)
        streamController = StreamController(realtime, peerConnectionFactory)
        stageHandshake?.connect(stageIdentifier) { event ->
            Timber.d("Streams: ${event.streams.map { it.type }}")
            event.streams.forEach { stream ->
                streamController?.connect(stream) { peerConnection, videoTrack, audioTrack ->
                    peerConnections[stream.type] = peerConnection
                    renderers[stream.type]?.let { videoTrack?.addSink(it) }
//                    audioTrack?.setVolume(0.125) //TODO: make it possible to control volume
                }
            }
        }
        messageHandshake = MessageHandshake(tokenStore)
        messageHandshake?.connect(stageIdentifier) {
            Timber.d("Received message (${it.type}) from ${it.publisher.username} (${it.publisher.name}): ${it.body.text}")
        }
    }

    private fun disconnectStreams() {
        stageHandshake?.close()
        streamController?.close()
        messageHandshake?.close()
        peerConnections.values.onEach { it.dispose() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        primary_view_renderer.release()
        secondary_view_renderer.release()
        renderers.clear()
        disconnectStreams()
    }

}


package tv.caffeine.app.stage


import android.content.Context
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
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

private const val PRIMARY = "primary"
private const val SECONDARY = "secondary"
private val ALL_STREAMS = arrayOf(PRIMARY, SECONDARY)

class StageFragment : DaggerFragment() {
    lateinit var stageIdentifier : String
    lateinit var broadcaster: String
    private val peerConnections: MutableMap<String, PeerConnection> = mutableMapOf()
    private val renderers: MutableMap<String, SurfaceViewRenderer> = mutableMapOf()
    var stageHandshake: StageHandshake? = null
    var messageHandshake: MessageHandshake? = null
    var streamController: StreamController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private val audioTracks: MutableMap<String, AudioTrack> = mutableMapOf()

    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var eventsService: EventsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        val args = StageFragmentArgs.fromBundle(arguments)
        stageIdentifier = args.stageIdentifier
        broadcaster = args.broadcaster
        connectStreams()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectStreams()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSurfaceViewRenderer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deinitSurfaceViewRenderers()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity?.volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onStart() {
        super.onStart()
        setMediaTracksEnabled(true)
    }

    override fun onStop() {
        super.onStop()
        setMediaTracksEnabled(false)
    }

    private fun initSurfaceViewRenderer() {
        renderers[PRIMARY] = primary_view_renderer
        renderers[SECONDARY] = secondary_view_renderer
        renderers.forEach { entry ->
            val key = entry.key
            val renderer = entry.value
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            renderer.setEnableHardwareScaler(true)
            videoTracks[key]?.addSink(renderer)
        }
    }

    private fun connectStreams() {
        stageHandshake = StageHandshake(tokenStore)
        streamController = StreamController(realtime, peerConnectionFactory, eventsService, stageIdentifier)
        stageHandshake?.connect(stageIdentifier) { event ->
            Timber.d("Streams: ${event.streams.map { it.type }}")
            event.streams.forEach { stream ->
                streamController?.connect(stream) { peerConnection, videoTrack, audioTrack ->
                    val streamType = stream.type
                    peerConnections[streamType] = peerConnection
                    renderers[streamType]?.let { videoTrack?.addSink(it) }
                    videoTrack?.let { videoTracks[streamType] = it }
                    audioTrack?.let { audioTracks[streamType] = it }
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

    private fun deinitSurfaceViewRenderers() {
        ALL_STREAMS.forEach {
            videoTracks[it]?.removeSink(renderers[it])
        }
        primary_view_renderer.release()
        secondary_view_renderer.release()
        renderers.clear()
    }

    private fun setMediaTracksEnabled(enabled: Boolean) {
        videoTracks.values.forEach { it.setEnabled(enabled) }
        audioTracks.values.forEach { it.setEnabled(enabled) }
    }

}


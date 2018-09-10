package tv.caffeine.app.stage


import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_stage.*
import org.webrtc.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

private val ALL_STREAMS = arrayOf(StageHandshake.Stream.Type.primary, StageHandshake.Stream.Type.secondary)

class StageFragment : DaggerFragment() {
    lateinit var stageIdentifier : String
    lateinit var broadcaster: String
    private val peerConnections: MutableMap<StageHandshake.Stream.Type, PeerConnection> = mutableMapOf()
    private val renderers: MutableMap<StageHandshake.Stream.Type, SurfaceViewRenderer> = mutableMapOf()
    var stageHandshake: StageHandshake? = null
    var messageHandshake: MessageHandshake? = null
    var streamController: StreamController? = null
    private val videoTracks: MutableMap<StageHandshake.Stream.Type, VideoTrack> = mutableMapOf()
    private val audioTracks: MutableMap<StageHandshake.Stream.Type, AudioTrack> = mutableMapOf()
    private var streams: Map<StageHandshake.Stream.Type, StageHandshake.Stream> = mapOf()

    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var eventsService: EventsService

    private val latestMessages: MutableList<MessageHandshake.Message> = mutableListOf()

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
        displayMessages()
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
        renderers[StageHandshake.Stream.Type.primary] = primary_view_renderer
        renderers[StageHandshake.Stream.Type.secondary] = secondary_view_renderer
        renderers.forEach { entry ->
            val key = entry.key
            val renderer = entry.value
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            renderer.setEnableHardwareScaler(true)
            configureRenderer(renderer, streams[key], videoTracks[key])
        }
    }

    private fun configureRenderer(renderer: SurfaceViewRenderer, stream: StageHandshake.Stream?, videoTrack: VideoTrack?) {
        val hasVideo = videoTrack != null && stream?.capabilities?.video ?: false
        renderer.isVisible = hasVideo
        if (hasVideo) {
            videoTrack?.addSink(renderer)
        }
    }

    private fun connectStreams() {
        stageHandshake = StageHandshake(tokenStore)
        streamController = StreamController(realtime, peerConnectionFactory, eventsService, stageIdentifier)
        stageHandshake?.connect(stageIdentifier) { event ->
            Timber.d("Streams: ${event.streams.map { it.type }}")
            streams = event.streams.associateBy { stream -> stream.type }
            event.streams.forEach { stream ->
                streamController?.connect(stream) { peerConnection, videoTrack, audioTrack ->
                    val streamType = stream.type
                    peerConnections[streamType] = peerConnection
                    videoTrack?.let { videoTracks[streamType] = it }
                    audioTrack?.let { audioTracks[streamType] = it }
                    renderers[streamType]?.let {
                        configureRenderer(it, stream, videoTrack)
                    }
                }
            }
        }
        messageHandshake = MessageHandshake(tokenStore)
        messageHandshake?.connect(stageIdentifier) { message ->
            val oldInstance = latestMessages.find { it.id == message.id }
            if (oldInstance != null) {
                latestMessages.remove(oldInstance)
            }
            latestMessages.add(message)
            Timber.d("Received message (${message.type}) from ${message.publisher.username} (${message.publisher.name}): ${message.body.text}")
            displayMessages()
        }
    }

    private fun disconnectStreams() {
        stageHandshake?.close()
        streamController?.close()
        messageHandshake?.close()
        peerConnections.values.onEach { it.dispose() }
    }

    private fun displayMessages() {
        val summary = latestMessages
                .takeLast(5)
                .joinToString("\n") {
                    "${it.publisher.username}: ${it.body.text}" + if (it.endorsementCount > 0) " <${it.endorsementCount}>" else ""
                }
        messagesTextView?.setText(summary)
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


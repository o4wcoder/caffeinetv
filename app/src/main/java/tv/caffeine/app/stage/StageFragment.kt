package tv.caffeine.app.stage


import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_stage.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.webrtc.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class StageFragment : DaggerFragment() {
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
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var broadcastsService: BroadcastsService

    private val latestMessages: MutableList<MessageHandshake.Message> = mutableListOf()
    private var job: Job? = null
    private var broadcastName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        val args = StageFragmentArgs.fromBundle(arguments)
        broadcaster = args.broadcaster
        job = launch {
            val userDetails = followManager.userDetails(broadcaster)
            val broadcastDetails = broadcastsService.broadcastDetails(userDetails.broadcastId)
            launch(UI) {
                connectStreams(userDetails.stageId)
                broadcastName = broadcastDetails.await().broadcast.name
                title = broadcastName
            }
        }
    }

    var title: String? = null
        set(value) {
            field = value
            (activity as? AppCompatActivity)?.supportActionBar?.title = value
        }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        disconnectStreams()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = broadcastName
        initSurfaceViewRenderer()
        displayMessages()
        configureButtons()
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

    private fun connectStreams(stageIdentifier: String) {
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
        messagesTextView?.text = summary
    }

    private fun configureButtons() {
        share_button?.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                val textToShare = broadcastName?.let { getString(R.string.watching_caffeine_live_with_description, it) } ?: getString(R.string.watching_caffeine_live)
                putExtra(Intent.EXTRA_TEXT, textToShare)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
        }
    }

    private fun deinitSurfaceViewRenderers() {
        renderers.forEach { entry ->
            val key = entry.key
            val renderer = entry.value
            videoTracks[key]?.removeSink(renderer)
            renderer.release()
        }
        renderers.clear()
    }

    private fun setMediaTracksEnabled(enabled: Boolean) {
        videoTracks.values.forEach { it.setEnabled(enabled) }
        audioTracks.values.forEach { it.setEnabled(enabled) }
    }

}


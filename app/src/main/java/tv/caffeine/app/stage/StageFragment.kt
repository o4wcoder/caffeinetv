package tv.caffeine.app.stage


import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.webrtc.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentStageBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.ui.showKeyboard
import javax.inject.Inject

class StageFragment : CaffeineFragment() {
    private lateinit var broadcaster: String
    private val peerConnections: MutableMap<String, PeerConnection> = mutableMapOf()
    private val renderers: MutableMap<StageHandshake.Stream.Type, SurfaceViewRenderer> = mutableMapOf()
    private val sinks: MutableMap<String, StageHandshake.Stream.Type> = mutableMapOf()
    private var stageHandshake: StageHandshake? = null
    private var messageHandshake: MessageHandshake? = null
    private var streamController: StreamController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private val audioTracks: MutableMap<String, AudioTrack> = mutableMapOf()
    private var streams: Map<String, StageHandshake.Stream> = mapOf()

    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var eventsService: EventsService
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var broadcastsService: BroadcastsService
    @Inject lateinit var usersService: UsersService

    @Inject lateinit var chatMessageAdapter: ChatMessageAdapter

    private val latestMessages: MutableList<Message> = mutableListOf()
    private var broadcastName: String? = null

    lateinit var binding: FragmentStageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        val args = StageFragmentArgs.fromBundle(arguments)
        broadcaster = args.broadcaster
        launch {
            val userDetails = followManager.userDetails(broadcaster)
            val broadcastDetails = userDetails.broadcastId?.let { broadcastsService.broadcastDetails(it) }
            launch(Dispatchers.Main) {
                connectStreams(userDetails.stageId)
                broadcastName = broadcastDetails?.await()?.broadcast?.name
                title = broadcastName
            }
            launch(Dispatchers.Main) {
                connectMessages(userDetails.stageId)
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
        disconnectStreams()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentStageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = broadcastName
        binding.messagesRecyclerView?.adapter = chatMessageAdapter
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
        renderers[StageHandshake.Stream.Type.primary] = binding.primaryViewRenderer
        renderers[StageHandshake.Stream.Type.secondary] = binding.secondaryViewRenderer
        renderers.forEach { entry ->
            val key = entry.key
            val renderer = entry.value
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            renderer.setEnableHardwareScaler(true)
            streams.values.firstOrNull { it.type == key }?.let { stream ->
                configureRenderer(renderer, stream, videoTracks[stream.id])
            }
        }
    }

    private fun configureRenderer(renderer: SurfaceViewRenderer, stream: StageHandshake.Stream?, videoTrack: VideoTrack?) {
        val hasVideo = videoTrack != null && stream?.capabilities?.video ?: false
        renderer.visibility = if (hasVideo) View.VISIBLE else View.INVISIBLE
        if (hasVideo) {
            videoTrack?.addSink(renderer)
        } else {
            videoTrack?.removeSink(renderer)
        }
    }

    private suspend fun connectStreams(stageIdentifier: String) {
        stageHandshake = StageHandshake(tokenStore, stageIdentifier)
        streamController = StreamController(realtime, peerConnectionFactory, eventsService, stageIdentifier)
        stageHandshake?.channel?.consumeEach { event ->
            Timber.d("Streams: ${event.streams.map { it.type }}")
            val newStreams = event.streams.associateBy { stream -> stream.id }
            Timber.d("StreamState - New streams: $newStreams")
            val oldStreams = streams
            streams = newStreams
            Timber.d("StreamState - Old streams: $oldStreams")
            val removedStreamIds = oldStreams.keys.subtract(newStreams.keys)
            removedStreamIds.forEach { streamId ->
                val stream = oldStreams[streamId]
                Timber.d("StreamState - Removed stream $streamId, ${stream?.type}, ${stream?.label}")
                peerConnections.remove(streamId)?.close()
                videoTracks.remove(streamId)?.apply {
                    removeSink(renderers[sinks[streamId]])
//                    dispose()
                }
                audioTracks.remove(streamId)//?.dispose()
                sinks.remove(streamId)
            }
            val addedStreamIds = newStreams.keys.subtract(oldStreams.keys)
            val updatedStreamIds = newStreams.keys.intersect(oldStreams.keys)
            newStreams.values.filter { it.id in updatedStreamIds }.forEach { Timber.d("Updated stream ${it.id}, ${it.type}, ${it.label}") }
            newStreams.values
                    .filter { newStream ->
                        oldStreams[newStream.id]?.let { oldStream ->
                            oldStream.type != newStream.type
                        } ?: false
                    }
                    .forEach { stream ->
                        Timber.d("StreamState - Switching stream ${stream.id}, ${stream.label} from ${oldStreams[stream.id]?.type} to ${stream.type}")
                        videoTracks[stream.id]?.removeSink(renderers[sinks[stream.id]])
                        sinks[stream.id] = stream.type
                        renderers[stream.type]?.let {
                            configureRenderer(it, stream, videoTracks[stream.id])
                        }
                    }
            newStreams.values.filter { it.id in addedStreamIds }.forEach { stream ->
                Timber.d("StreamState - Configuring new stream ${stream.id}, ${stream.type}, ${stream.label}")
                streamController?.connect(stream) { peerConnection, videoTrack, audioTrack ->
                    val streamId = stream.id
                    val streamType = stream.type
                    peerConnections[streamId] = peerConnection
                    videoTrack?.let { videoTracks[streamId] = it }
                    audioTrack?.let { audioTracks[streamId] = it }
                    sinks[streamId] = streamType
                    renderers[streamType]?.let {
                        configureRenderer(it, stream, videoTrack)
                    }
                }
            }
        }
    }

    private suspend fun connectMessages(stageIdentifier: String) {
        messageHandshake = MessageHandshake(tokenStore, stageIdentifier)
        messageHandshake?.channel?.consumeEach { message ->
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
        stageHandshake = null
        streamController?.close()
        streamController = null
        messageHandshake?.close()
        messageHandshake = null
        peerConnections.values.onEach { it.dispose() }
    }

    private fun displayMessages() {
        val summary = latestMessages
                .takeLast(4)
                .joinToString("\n") {
                    "${it.publisher.username}: ${it.body.text}" + if (it.endorsementCount > 0) " <${it.endorsementCount}>" else ""
                }
        Timber.d("Chat messages: $summary")
        chatMessageAdapter.submitList(latestMessages.takeLast(4))
    }

    private fun configureButtons() {
        binding.shareButton?.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                val textToShare = broadcastName?.let { getString(R.string.watching_caffeine_live_with_description, it) } ?: getString(R.string.watching_caffeine_live)
                putExtra(Intent.EXTRA_TEXT, textToShare)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
        }
        binding.chatButton?.setOnClickListener {
            binding.chatMessageEditText?.requestFocus()
            binding.chatMessageEditText?.showKeyboard()
            binding.chatMessageEditText?.setOnAction(EditorInfo.IME_ACTION_SEND) {
                sendMessage()
            }
        }
        binding.friendsWatchingButton?.setOnClickListener {
            val fragment = FriendsWatchingFragment()
            val action = StageFragmentDirections.actionStageFragmentToFriendsWatchingFragment(broadcaster)
            fragment.arguments = action.arguments
            fragment.show(fragmentManager, "FW")
        }
        binding.giftButton?.setOnClickListener {
            val fragment = DICatalogFragment()
            val action = StageFragmentDirections.actionStageFragmentToDigitalItemListDialogFragment(broadcaster)
            fragment.arguments = action.arguments
            fragment.show(fragmentManager, "DI")
        }
    }

    private fun sendMessage() {
        val editText = binding.chatMessageEditText ?: return
        val text = editText.text.toString()
        editText.text = null
        launch {
            val userDetails = followManager.userDetails(broadcaster)
            val caid = tokenStore.caid ?: error("Not logged in")
            val signedUserDetails = usersService.signedUserDetails(caid)
            val publisher = signedUserDetails.await().token
            val stageId = userDetails.stageId
            val message = Reaction("reaction", publisher, Message.Body(text))
            val deferred = realtime.sendMessage(stageId, message)
            val result = deferred.await()
            Timber.d("Sent message $text with result $result")
        }
    }

    private fun deinitSurfaceViewRenderers() {
        for (renderer in renderers.values) {
            for (videoTrack in videoTracks.values) {
                videoTrack.removeSink(renderer)
            }
            renderer.release()
        }
        renderers.clear()
    }

    private fun setMediaTracksEnabled(enabled: Boolean) {
        videoTracks.values.forEach { it.setEnabled(enabled) }
        audioTracks.values.forEach { it.setEnabled(enabled) }
    }

}


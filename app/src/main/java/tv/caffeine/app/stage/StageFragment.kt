package tv.caffeine.app.stage

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.*
import timber.log.Timber
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.*
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentStageBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.receiver.HeadsetBroadcastReceiver
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.htmlText
import tv.caffeine.app.util.*
import javax.inject.Inject
import kotlin.collections.set

private const val PICK_DIGITAL_ITEM = 0
private const val SEND_MESSAGE = 1

class StageFragment : CaffeineFragment(), DICatalogFragment.Callback, SendMessageFragment.Callback {

    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var eventsService: EventsService
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var broadcastsService: BroadcastsService
    @Inject lateinit var usersService: UsersService
    @Inject lateinit var chatMessageAdapter: ChatMessageAdapter
    @Inject lateinit var gson: Gson

    private lateinit var binding: FragmentStageBinding
    private lateinit var broadcaster: String
    private val peerConnections: MutableMap<String, PeerConnection> = mutableMapOf()
    private val renderers: MutableMap<StageHandshake.Stream.Type, SurfaceViewRenderer> = mutableMapOf()
    private val loadingIndicators: MutableMap<StageHandshake.Stream.Type, ProgressBar> = mutableMapOf()
    private val sinks: MutableMap<String, StageHandshake.Stream.Type> = mutableMapOf()
    private var stageHandshake: StageHandshake? = null
    private var streamController: StreamController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private val audioTracks: MutableMap<String, AudioTrack> = mutableMapOf()
    private var streams: Map<String, StageHandshake.Stream> = mapOf()
    private var broadcastName: String? = null
    private val broadcastReceiver = HeadsetBroadcastReceiver()
    private var audioManager: AudioManager? = null
    private var wasSpeakerOn = false

    private val chatViewModel: ChatViewModel by lazy { viewModelProvider.get(ChatViewModel::class.java) }
    private val profileViewModel by lazy { viewModelProvider.get(ProfileViewModel::class.java) }

    private var isFollowingBroadcaster = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        val args = StageFragmentArgs.fromBundle(arguments)
        broadcaster = args.broadcaster
        audioManager = context?.getSystemService()
        wasSpeakerOn = audioManager?.isSpeakerphoneOn ?: false
        audioManager?.isSpeakerphoneOn = true
        context?.let { LocalBroadcastManager.getInstance(it).registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG)) }
        launch {
            val userDetails = followManager.userDetails(broadcaster) ?: return@launch
            launch {
                followManager.refreshFollowedUsers()
                isFollowingBroadcaster = followManager.isFollowing(broadcaster)
            }
            launch(dispatchConfig.main) {
                connectStreams(userDetails.stageId)
            }
            launch(dispatchConfig.main) {
                connectMessages(userDetails.stageId)
            }
        }
    }

    private var title: String? = null
        set(value) {
            field = value
            binding.stageToolbar.title = value
        }

    override fun onDestroy() {
        disconnectStreams()
        context?.let { LocalBroadcastManager.getInstance(it).unregisterReceiver(broadcastReceiver) }
        audioManager?.isSpeakerphoneOn = wasSpeakerOn
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        activity?.apply {
            setDarkMode(true)
            setImmersiveSticky()
        }
        // Inflate the layout for this fragment
        binding = FragmentStageBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    private var viewJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val profileAvatarTransform = CropBorderedCircleTransformation(resources.getColor(R.color.caffeine_blue, null),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics))
        viewJob = launch {
            launch(dispatchConfig.main) {
                val userDetails = followManager.userDetails(broadcaster)
                if (userDetails != null) {
                    profileViewModel.load(userDetails.caid)
                    binding.profileViewModel = profileViewModel
                    binding.stageToolbar.apply {
                        inflateMenu(R.menu.stage_menu)
                        menu.findItem(R.id.stage_overflow_menu).setOnMenuItemClickListener {
                            if (it.itemId == R.id.stage_overflow_menu) {
                                fragmentManager?.navigateToReportOrIgnoreDialog(
                                        userDetails.caid, userDetails.username, true
                                )
                            }
                            true
                        }
                    }
                    profileViewModel.username.observe(viewLifecycleOwner, Observer { username ->
                        binding.showIsOverTextView.htmlText = getString(R.string.broadcaster_show_is_over, username)
                    })
                }
            }
            launch {
                while(isActive) {
                    val userDetails = followManager.loadUserDetails(broadcaster)
                    if (userDetails != null) {
                        val broadcastId = userDetails.broadcastId ?: break
                        updateBroadcastDetails(broadcastId)
                        updateFriendsWatching(broadcastId, profileAvatarTransform)
                    }
                    delay(5000L)
                }
            }
        }
        val navController = findNavController()
        binding.stageToolbar.setupWithNavController(navController, null)
        title = broadcastName
        binding.messagesRecyclerView?.adapter = chatMessageAdapter
        chatMessageAdapter.callback = object: ChatMessageAdapter.Callback {
            override fun replyClicked(message: Message) {
                val string = getString(R.string.username_prepopulated_reply, message.publisher.username)
                openSendMessage(string)
            }

            override fun upvoteClicked(message: Message) {
                chatViewModel.endorseMessage(message)
            }
        }
        initSurfaceViewRenderer()
        configureButtons()
    }

    override fun onDestroyView() {
        viewJob?.cancel()
        viewJob = null
        deinitSurfaceViewRenderers()
        activity?.apply {
            unsetImmersiveSticky()
            setDarkMode(false)

            getPreferences(Context.MODE_PRIVATE)?.let {
                val key = getString(R.string.is_first_time_on_stage)
                if (it.getBoolean(key, true)) {
                    // This will re-enable the immersive mode function in MainActivity.onWindowFocusChanged().
                    it.edit().putBoolean(key, false).apply()
                }
            }
        }
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
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
        loadingIndicators[StageHandshake.Stream.Type.primary] = binding.primaryLoadingIndicator
        loadingIndicators[StageHandshake.Stream.Type.secondary] = binding.secondaryLoadingIndicator
        renderers.forEach { entry ->
            val key = entry.key
            val renderer = entry.value
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            renderer.setEnableHardwareScaler(true)
            streams.values.firstOrNull { it.type == key }?.let { stream ->
                configureRenderer(renderer, stream, videoTracks[stream.id])
            }
            renderer.setOnClickListener { toggleAppBarVisibility() }
        }
    }

    private var appBarVisibilityJob: Job? = null

    private fun toggleAppBarVisibility() {
        val viewsToToggle = listOf(binding.stageAppbar, binding.gameLogoImageView, binding.liveIndicatorAndAvatarContainer)
        appBarVisibilityJob?.cancel()
        val currentVisibility = binding.stageAppbar.visibility
        if (currentVisibility != View.VISIBLE) {
            viewsToToggle.forEach {
                it.visibility = View.VISIBLE
            }
            if (!isFollowingBroadcaster) binding.followButton.isVisible = true
            appBarVisibilityJob = launch {
                delay(3000)
                viewsToToggle.forEach {
                    it.visibility = View.INVISIBLE
                }
                binding.followButton.isVisible = false
            }
        } else {
            viewsToToggle.forEach {
                it.visibility = View.INVISIBLE
            }
            binding.followButton.isVisible = false
        }
    }

    private suspend fun updateBroadcastDetails(broadcastId: String) {
        val result = broadcastsService.broadcastDetails(broadcastId).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> {
                val broadcast = result.value.broadcast
                broadcastName = broadcast.name
                title = broadcastName
                Picasso.get()
                        .load(broadcast.game?.iconImageUrl)
                        .into(binding.gameLogoImageView)
                updateShowIsOverVisibility(broadcast.isOnline())
            }
            is CaffeineResult.Error -> Timber.e("Error loading broadcast details ${result.error}")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }

    private suspend fun updateFriendsWatching(broadcastId: String, profileAvatarTransform: CropBorderedCircleTransformation) {
        val result = broadcastsService.friendsWatching(broadcastId).awaitAndParseErrors(gson)
        if (binding.friendsWatchingButton == null) return
        when (result) {
            is CaffeineResult.Success -> {
                val friendAvatarImageUrl = result.value.firstOrNull()?.let { followManager.userDetails(it.caid)?.avatarImageUrl }
                if (friendAvatarImageUrl == null) {
                    binding.friendsWatchingButton?.isEnabled = false
                    binding.friendsWatchingButton?.setImageDrawable(null)
                } else {
                    binding.friendsWatchingButton?.isEnabled = true
                    binding.friendsWatchingButton?.imageTintList = null
                    Picasso.get().load(friendAvatarImageUrl)
                            .resizeDimen(R.dimen.toolbar_icon_size, R.dimen.toolbar_icon_size)
                            .placeholder(R.drawable.ic_profile)
                            .transform(profileAvatarTransform)
                            .into(binding.friendsWatchingButton)
                }
            }
            is CaffeineResult.Error -> Timber.e("Failed to fetch friends watching ${result.error}")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }

    private fun updateShowIsOverVisibility(broadcastIsOnline: Boolean) {
        binding.largeAvatarImageView.isVisible = !broadcastIsOnline
        binding.showIsOverTextView.isVisible = !broadcastIsOnline
        binding.backToLobbyButton.isVisible = !broadcastIsOnline
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
        stageHandshake = StageHandshake(dispatchConfig, tokenStore, stageIdentifier)
        streamController = StreamController(dispatchConfig, realtime, peerConnectionFactory, eventsService, gson, stageIdentifier)
        stageHandshake?.channel?.consumeEach { event ->
            Timber.d("Streams: ${event.streams.map { it.type }}")
            binding.liveIndicatorTextView.isVisible = event.streams.isNotEmpty()
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
                    val renderer = renderers[sinks[streamId]] ?: return@apply
                    removeSink(renderer)
                    renderer.visibility = View.INVISIBLE
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
                val streamType = stream.type
                loadingIndicators[streamType]?.isVisible = true
                val connectionInfoNullable = streamController?.connect(stream)
                loadingIndicators[streamType]?.isVisible = false
                val connectionInfo = connectionInfoNullable ?: return@forEach
                val peerConnection = connectionInfo.peerConnection
                val videoTrack = connectionInfo.videoTrack
                val audioTrack = connectionInfo.audioTrack
                val streamId = stream.id
                peerConnections[streamId] = peerConnection
                videoTrack?.let { videoTracks[streamId] = it }
                audioTrack?.let { audioTracks[streamId] = it }
                sinks[streamId] = streamType
                renderers[streamType]?.let {
                    configureRenderer(it, stream, videoTrack)
                    it.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun connectMessages(stageIdentifier: String) {
        chatViewModel.load(stageIdentifier)
        chatViewModel.messages.observe(this, Observer { messages ->
            chatMessageAdapter.submitList(messages)
        })
    }

    private fun disconnectStreams() {
        stageHandshake?.close()
        stageHandshake = null
        streamController?.close()
        streamController = null
        peerConnections.values.onEach { it.dispose() }
    }

    private fun configureButtons() {
        binding.shareButton?.setOnClickListener {
            profileViewModel.username.observe(viewLifecycleOwner, Observer { username ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    val textToShare = broadcastName?.let { getString(R.string.watching_caffeine_live_with_description, username, it) } ?: getString(R.string.watching_caffeine_live)
                    putExtra(Intent.EXTRA_TEXT, textToShare)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
            })
        }
        binding.chatButton?.setOnClickListener { openSendMessage() }
        binding.friendsWatchingButton?.setOnClickListener {
            val fragmentManager = fragmentManager ?: return@setOnClickListener
            val fragment = FriendsWatchingFragment()
            val action = StageFragmentDirections.actionStageFragmentToFriendsWatchingFragment(broadcaster)
            fragment.arguments = action.arguments
            fragment.show(fragmentManager, "FW")
        }
        binding.giftButton?.setOnClickListener {
            sendDigitalItemWithMessage(null)
        }
        binding.avatarImageView.setOnClickListener {
            findNavController().safeNavigate(LobbyDirections.actionGlobalProfileFragment(broadcaster))
        }
        binding.followButton.setOnClickListener {
            launch {
                val result = followManager.followUser(broadcaster)
                when(result) {
                    is CaffeineEmptyResult.Success -> {
                        isFollowingBroadcaster = true
                        binding.followButton.isVisible = false
                    }
                    is CaffeineEmptyResult.Error -> {
                        if (result.error.isMustVerifyEmailError()) {
                            val fragment = AlertDialogFragment.withMessage(R.string.verify_email_to_follow_more_users)
                            fragment.show(fragmentManager, "verifyEmail")
                        } else {
                            Timber.e("Couldn't follow user ${result.error}")
                        }
                    }
                    is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
                }
            }
        }
        binding.backToLobbyButton.setOnClickListener {
            findNavController().popBackStack(R.id.lobbyFragment, false)
        }
    }

    override fun sendDigitalItemWithMessage(message: String?) {
        val fragmentManager = fragmentManager ?: return
        val fragment = DICatalogFragment()
        val action = StageFragmentDirections.actionStageFragmentToDigitalItemListDialogFragment(broadcaster, message)
        fragment.setTargetFragment(this, PICK_DIGITAL_ITEM)
        fragment.arguments = action.arguments
        fragment.show(fragmentManager, "DI")
    }

    override fun sendMessage(message: String?) {
        val text = message ?: return
        chatViewModel.sendMessage(text, broadcaster)
    }

    private fun openSendMessage(message: String? = null) {
        val fragmentManager = fragmentManager ?: return
        val fragment = SendMessageFragment()
        val action = StageFragmentDirections.actionStageFragmentToSendMessageFragment(message)
        fragment.arguments = action.arguments
        fragment.show(fragmentManager, "sendMessage")
        fragment.setTargetFragment(this, SEND_MESSAGE)
    }

    override fun digitalItemSelected(digitalItem: DigitalItem, message: String?) {
        fragmentManager?.let { fm ->
            val fragment = SendDigitalItemFragment()
            val action = StageFragmentDirections.actionStageFragmentToSendDigitalItemFragment(digitalItem.id, broadcaster, message)
            fragment.arguments = action.arguments
            fragment.show(fm, "sendDigitalItem")
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

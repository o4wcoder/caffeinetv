package tv.caffeine.app.stage

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.MediaCodecVideoDecoder
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.VersionCheckError
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.iconImageUrl
import tv.caffeine.app.api.model.isOnline
import tv.caffeine.app.databinding.FragmentStageBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.session.SessionCheckViewModel
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.update.IsVersionSupportedCheckUseCase
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.broadcasterUsername
import tv.caffeine.app.util.isNetworkAvailable
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.safeUnregisterNetworkCallback
import tv.caffeine.app.util.setDarkMode
import tv.caffeine.app.util.setImmersiveSticky
import tv.caffeine.app.util.showSnackbar
import tv.caffeine.app.util.unsetImmersiveSticky
import javax.inject.Inject
import kotlin.collections.set

private const val PICK_DIGITAL_ITEM = 0
private const val SEND_MESSAGE = 1

class StageFragment : CaffeineFragment(), DICatalogFragment.Callback, SendMessageFragment.Callback {

    @Inject lateinit var factory: NewReyesController.Factory
    @Inject lateinit var eglBase: EglBase
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var broadcastsService: BroadcastsService
    @Inject lateinit var chatMessageAdapter: ChatMessageAdapter
    @Inject lateinit var gson: Gson
    @Inject lateinit var isVersionSupportedCheckUseCase: IsVersionSupportedCheckUseCase
    @Inject lateinit var picasso: Picasso

    @VisibleForTesting lateinit var binding: FragmentStageBinding
    private lateinit var broadcasterUsername: String
    private val renderers: MutableMap<NewReyes.Feed.Role, SurfaceViewRenderer> = mutableMapOf()
    private val loadingIndicators: MutableMap<NewReyes.Feed.Role, ProgressBar> = mutableMapOf()
    private var newReyesController: NewReyesController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private var feeds: Map<String, NewReyes.Feed> = mapOf()
    private var broadcastName: String? = null

    private val sessionCheckViewModel: SessionCheckViewModel by viewModels { viewModelFactory }
    private val chatViewModel: ChatViewModel by viewModels { viewModelFactory }
    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { viewModelFactory }

    private var isFollowingBroadcaster = false
    private var isMe = false
    private val args by navArgs<StageFragmentArgs>()
    @VisibleForTesting var stageIsLive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcasterUsername = args.broadcasterUsername()
        if (!MediaCodecVideoDecoder.isH264HwSupported()) {
            Timber.e(Exception("Failed to decode H264"))
            findNavController().safeNavigate(MainNavDirections.actionGlobalHardwareNotSupportedFragment())
            return
        }
        sessionCheckViewModel.sessionCheck.observe(this, Observer { result ->
            handle(result) {}
        })
        retainInstance = true
        context?.getSystemService<ConnectivityManager>()?.registerNetworkCallback(
                NetworkRequest.Builder().build(), networkCallback)

        launch {
            val isVersionSupported = isVersionSupportedCheckUseCase()
            if (isVersionSupported is CaffeineEmptyResult.Error) {
                handleError(CaffeineResult.Error<ApiErrorResult>(VersionCheckError()))
                return@launch
            }
            connectStage()
        }
    }

    private var connectStageJob: Job? = null

    private fun connectStage() {
        if (connectStageJob == null) {
            connectStageJob = launch {
                val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
                launch {
                    followManager.refreshFollowedUsers()
                    isFollowingBroadcaster = followManager.isFollowing(userDetails.caid)
                }
                connectStreams(userDetails.username)
                launch(dispatchConfig.main) {
                    connectMessages(userDetails.stageId)
                    connectFriendsWatching(userDetails.stageId)
                }
            }
        }
    }

    private fun disconnectStage() {
        connectStageJob?.cancel()
        connectStageJob = null
        disconnectStreams()
        chatViewModel.disconnect()
        friendsWatchingViewModel.disconnect()
    }

    private var title: String? = null
        set(value) {
            field = value
            binding.stageToolbar.title = value
        }

    override fun onDestroy() {
        disconnectStage()
        context?.getSystemService<ConnectivityManager>()?.safeUnregisterNetworkCallback(networkCallback)
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
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    private var viewJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnClickListener { toggleAppBarVisibility() }
        viewJob = launch {
            launch(dispatchConfig.main) {
                val userDetails = followManager.userDetails(broadcasterUsername)
                if (userDetails != null) {
                    profileViewModel.load(userDetails.caid)
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
                    profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
                        binding.userProfile = userProfile
                        binding.showIsOverTextView.formatUsernameAsHtml(picasso, getString(R.string.broadcaster_show_is_over, userProfile.username))

                    })

                    isMe = followManager.isSelf(userDetails.caid)
                    updateViewsOnMyStageVisibility()
                }
            }
            launch {
                while(isActive) {
                    val userDetails = followManager.loadUserDetails(broadcasterUsername)
                    if (userDetails != null) {
                        val broadcastId = userDetails.broadcastId ?: break
                        updateBroadcastDetails(broadcastId)
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

    override fun onStart() {
        super.onStart()
        connectStage()
    }

    private fun isChangingConfigurations() = activity?.isChangingConfigurations != false

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations()) disconnectStage()
    }

    private fun initSurfaceViewRenderer() {
        renderers[NewReyes.Feed.Role.primary] = binding.primaryViewRenderer
        renderers[NewReyes.Feed.Role.secondary] = binding.secondaryViewRenderer
        binding.secondaryViewRenderer.setZOrderMediaOverlay(true)
        loadingIndicators[NewReyes.Feed.Role.primary] = binding.primaryLoadingIndicator
        loadingIndicators[NewReyes.Feed.Role.secondary] = binding.secondaryLoadingIndicator
        renderers.forEach { entry ->
            val key = entry.key
            val renderer = entry.value
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            renderer.setEnableHardwareScaler(true)
            feeds.values.firstOrNull { it.role == key }?.let { feed ->
                configureRenderer(renderer, feed, videoTracks[feed.stream.id])
            }
            renderer.setOnClickListener { toggleAppBarVisibility() }
        }
    }

    private var appBarVisibilityJob: Job? = null

    @VisibleForTesting
    fun toggleAppBarVisibility() {
        appBarVisibilityJob?.cancel()
        if (!binding.stageAppbar.isVisible) {
            showOverlays()
            appBarVisibilityJob = launch {
                delay(3000)
                hideOverlays()
            }
        } else {
            hideOverlays()
        }
    }

    @VisibleForTesting
    fun showOverlays() = setAppBarVisible(true)

    @VisibleForTesting
    fun hideOverlays() = setAppBarVisible(false)

    @VisibleForTesting
    fun setAppBarVisible(visible: Boolean) {
        val viewsToToggle = listOf(binding.stageAppbar, binding.liveIndicatorAndAvatarContainer)
        val viewsForLiveOnly = listOf(binding.gameLogoImageView, binding.liveIndicatorTextView)
        if (visible) {
            viewsToToggle.forEach {
                it.isVisible = true
            }
            viewsForLiveOnly.forEach {
                it.isVisible = stageIsLive
            }
            if (!isFollowingBroadcaster && !isMe) binding.followButton.isVisible = true
        } else {
            viewsToToggle.plus(viewsForLiveOnly).forEach {
                it.isInvisible = true
            }
            binding.followButton.isVisible = false
        }
    }

    private fun updateViewsOnMyStageVisibility() {
        listOf(binding.giftButton, binding.friendsWatchingButton, binding.followButton, binding.avatarImageView).forEach {
            it?.isVisible = !isMe
        }
    }

    private suspend fun updateBroadcastDetails(broadcastId: String) {
        val result = broadcastsService.broadcastDetails(broadcastId).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> {
                val broadcast = result.value.broadcast
                broadcastName = broadcast.name
                title = broadcastName
                picasso
                        .load(broadcast.game?.iconImageUrl)
                        .into(binding.gameLogoImageView)
                updateBroadcastOnlineState(broadcast.isOnline())
            }
            is CaffeineResult.Error -> Timber.e("Error loading broadcast details ${result.error}")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }

    private fun updateFriendsWatching(friendsWatching: List<User>, profileAvatarTransform: CropBorderedCircleTransformation) {
        if (binding.friendsWatchingButton == null) return
        val friendAvatarImageUrl = friendsWatching.firstOrNull()?.avatarImageUrl
        if (friendAvatarImageUrl == null) {
            binding.friendsWatchingButton?.isEnabled = false
            binding.friendsWatchingButton?.setImageDrawable(null)
        } else {
            binding.friendsWatchingButton?.isEnabled = true
            binding.friendsWatchingButton?.imageTintList = null
            picasso.load(friendAvatarImageUrl)
                    .resizeDimen(R.dimen.toolbar_icon_size, R.dimen.toolbar_icon_size)
                    .placeholder(R.drawable.ic_profile)
                    .transform(profileAvatarTransform)
                    .into(binding.friendsWatchingButton)
        }
    }

    @VisibleForTesting
    fun updateBroadcastOnlineState(broadcastIsOnline: Boolean) {
        stageIsLive = broadcastIsOnline
        if (!broadcastIsOnline) showOverlays()
        binding.largeAvatarImageView.isVisible = !broadcastIsOnline
        binding.showIsOverTextView.isVisible = !broadcastIsOnline
        binding.backToLobbyButton.isVisible = !broadcastIsOnline
    }

    private fun configureRenderer(renderer: SurfaceViewRenderer, feed: NewReyes.Feed?, videoTrack: VideoTrack?) {
        val hasVideo = videoTrack != null && (feed?.capabilities?.video ?: false)
        renderer.isInvisible = !hasVideo
        if (hasVideo) {
            videoTrack?.addSink(renderer)
        } else {
            videoTrack?.removeSink(renderer)
        }
    }

    private fun connectStreams(username: String) {
        val controller = factory.create(username)
        newReyesController = controller
        manageFeeds(controller)
        manageStateChange(controller)
        manageConnections(controller)
        manageErrors(controller)
    }

    private fun manageFeeds(controller: NewReyesController) = launch {
        controller.feedChannel.consumeEach { mapOfFeeds ->
            feeds = mapOfFeeds
            val activeRoles = feeds.values.map { it.role }.toList()
            renderers[NewReyes.Feed.Role.primary]?.isInvisible = NewReyes.Feed.Role.primary !in activeRoles
            renderers[NewReyes.Feed.Role.secondary]?.isInvisible = NewReyes.Feed.Role.secondary !in activeRoles
        }
    }

    private fun manageStateChange(controller: NewReyesController) = launch {
        controller.stateChangeChannel.consumeEach { list ->
            list.forEach { stateChange ->
                when (stateChange) {
                    is NewReyesController.StateChange.FeedRemoved -> {
                        videoTracks.remove(stateChange.streamId)?.apply {
                            val renderer = renderers[stateChange.role] ?: return@apply
                            removeSink(renderer)
                            renderer.clearImage()
                        }
                    }
                    is NewReyesController.StateChange.FeedAdded -> {
                        loadingIndicators[stateChange.feed.role]?.isVisible = true
                    }
                    is NewReyesController.StateChange.FeedRoleChanged -> {
                        val oldRenderer = renderers[stateChange.oldRole]
                        videoTracks[stateChange.newFeed.stream.id]?.removeSink(oldRenderer)
                        oldRenderer?.clearImage()
                        renderers[stateChange.newFeed.role]?.let { renderer ->
                            configureRenderer(renderer, stateChange.newFeed, videoTracks[stateChange.newFeed.stream.id])
                        }
                    }
                    is NewReyesController.StateChange.FeedStreamChanged -> {
                        val oldRenderer = renderers[stateChange.role]
                        videoTracks[stateChange.oldStreamId]?.removeSink(oldRenderer)
                        oldRenderer?.clearImage()
                    }
                }
            }
        }
    }

    private fun manageConnections(controller: NewReyesController) = launch {
        controller.connectionChannel.consumeEach { feedInfo ->
            loadingIndicators[feedInfo.role]?.isVisible = false
            val connectionInfo = feedInfo.connectionInfo
            val videoTrack = connectionInfo.videoTrack
            renderers[feedInfo.role]?.let {
                configureRenderer(it, feedInfo.feed, videoTrack)
            }
            val streamId = feedInfo.streamId
            videoTrack?.let { videoTracks[streamId] = it }
            renderers[feedInfo.role]?.let {
                configureRenderer(it, feedInfo.feed, videoTrack)
            }
        }
    }

    private fun manageErrors(controller: NewReyesController) = launch {
        controller.errorChannel.consumeEach { error ->
            when(error) {
                is NewReyesController.Error.PeerConnectionError -> activity?.showSnackbar(R.string.peer_connection_error_message)
                is NewReyesController.Error.OutOfCapacity -> findNavController().safeNavigate(MainNavDirections.actionGlobalOutOfCapacityFragment())
            }
        }
    }

    private fun connectMessages(stageIdentifier: String) {
        chatViewModel.load(stageIdentifier)
        chatViewModel.messages.observe(this, Observer { messages ->
            chatMessageAdapter.submitList(messages)
        })
    }

    private fun connectFriendsWatching(stageIdentifier: String) {
        val profileAvatarTransform = CropBorderedCircleTransformation(resources.getColor(R.color.caffeine_blue, null),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics))
        friendsWatchingViewModel.load(stageIdentifier)
        friendsWatchingViewModel.friendsWatching.observe(this, Observer { friendsWatching ->
            updateFriendsWatching(friendsWatching, profileAvatarTransform)
        })
        binding.friendsWatchingButton?.setOnClickListener {
            val fragmentManager = fragmentManager ?: return@setOnClickListener
            val fragment = FriendsWatchingFragment()
            val action = StageFragmentDirections.actionStageFragmentToFriendsWatchingFragment(stageIdentifier)
            fragment.arguments = action.arguments
            fragment.show(fragmentManager, "FW")
        }
    }

    private fun disconnectStreams() {
        newReyesController?.close()
        newReyesController = null
        videoTracks.clear()
    }

    private fun configureButtons() {
        binding.shareButton?.setOnClickListener {
            profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    val textToShare = broadcastName?.let { getString(R.string.watching_caffeine_live_with_description, userProfile.username, it) } ?: getString(R.string.watching_caffeine_live)
                    putExtra(Intent.EXTRA_TEXT, textToShare)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
            })
        }
        binding.chatButton?.setOnClickListener { openSendMessage() }
        binding.giftButton?.setOnClickListener {
            sendDigitalItemWithMessage(null)
        }
        binding.avatarImageView.setOnClickListener {
            findNavController().safeNavigate(MainNavDirections.actionGlobalProfileFragment(broadcasterUsername))
        }
        binding.followButton.setOnClickListener {
            launch {
                val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
                val result = followManager.followUser(userDetails.caid)
                when(result) {
                    is CaffeineEmptyResult.Success -> {
                        isFollowingBroadcaster = true
                        binding.followButton.isVisible = false
                    }
                    is CaffeineEmptyResult.Error -> {
                        if (result.error.isMustVerifyEmailError()) {
                            val fragment = AlertDialogFragment.withMessage(R.string.verify_email_to_follow_more_users)
                            fragment.maybeShow(fragmentManager, "verifyEmail")
                        } else {
                            Timber.e("Couldn't follow user ${result.error}")
                        }
                    }
                    is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
                }
            }
        }
        binding.backToLobbyButton.setOnClickListener {
            findNavController().popBackStack(R.id.lobbySwipeFragment, false)
        }
    }

    override fun sendDigitalItemWithMessage(message: String?) {
        val fragmentManager = fragmentManager ?: return
        val fragment = DICatalogFragment()
        val action = StageFragmentDirections.actionStageFragmentToDigitalItemListDialogFragment(broadcasterUsername, message)
        fragment.setTargetFragment(this, PICK_DIGITAL_ITEM)
        fragment.arguments = action.arguments
        fragment.show(fragmentManager, "DI")
    }

    override fun sendMessage(message: String?) {
        val text = message ?: return
        chatViewModel.sendMessage(text, broadcasterUsername)
    }

    private fun openSendMessage(message: String? = null) {
        val fragmentManager = fragmentManager ?: return
        val fragment = SendMessageFragment()
        val action = StageFragmentDirections.actionStageFragmentToSendMessageFragment(message, !isMe)
        fragment.arguments = action.arguments
        fragment.show(fragmentManager, "sendMessage")
        fragment.setTargetFragment(this, SEND_MESSAGE)
    }

    override fun digitalItemSelected(digitalItem: DigitalItem, message: String?) {
        val fm = fragmentManager ?: return
        launch {
            val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
            val fragment = SendDigitalItemFragment()
            val action = StageFragmentDirections.actionStageFragmentToSendDigitalItemFragment(digitalItem.id, userDetails.caid, message)
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

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        private var wasNetworkLost = false

        override fun onAvailable(network: Network?) {
            super.onAvailable(network)
            if (wasNetworkLost) {
                wasNetworkLost = false
                launch {
                    connectStage()
                }
            }
        }

        /**
         * 1. AP on, wifi off -> stage -> wifi on -> AP off -> isNetworkAvailable = true -> connectStage()
         * 2. AP on, wifi on -> stage -> wifi off -> isNetworkAvailable = false -> onAvailable() -> connectStage()
         * 3. Wifi on, AP on/off -> stage -> AP off/on -> no callbacks
         *
         * There is a potential Android bug in scenario #1 after the "wifi on" step.
         * The data is still being funneled through AP, but Android thinks wifi is the active network.
         * When we turn off AP, we need to disconnect the stage on AP and re-connect it on wifi.
         */
        override fun onLost(network: Network?) {
            super.onLost(network)
            wasNetworkLost = true
            disconnectStage()
            if (context?.isNetworkAvailable() == true) {
                launch {
                    connectStage()
                }
            }
        }
    }
}

package tv.caffeine.app.stage

import android.os.Bundle
import android.text.Spannable
import android.util.TypedValue
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.VisibleForTesting
import androidx.core.text.HtmlCompat
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
import org.threeten.bp.Clock
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.api.NewReyes
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
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.broadcasterUsername
import tv.caffeine.app.util.getHexColor
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject
import kotlin.collections.set

private const val PICK_DIGITAL_ITEM = 0
private const val SEND_MESSAGE = 1

class StageFragment : CaffeineFragment(R.layout.fragment_stage), DICatalogFragment.Callback, SendMessageFragment.Callback {

    @Inject lateinit var factory: NewReyesController.Factory
    @Inject lateinit var eglBase: EglBase
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var broadcastsService: BroadcastsService
    @Inject lateinit var chatMessageAdapter: ChatMessageAdapter
    @Inject lateinit var gson: Gson
    @Inject lateinit var picasso: Picasso
    @Inject lateinit var clock: Clock

    @VisibleForTesting lateinit var binding: FragmentStageBinding
    private lateinit var broadcasterUsername: String
    private val renderers: MutableMap<NewReyes.Feed.Role, SurfaceViewRenderer> = mutableMapOf()
    private val loadingIndicators: MutableMap<NewReyes.Feed.Role, ProgressBar> = mutableMapOf()
    private var newReyesController: NewReyesController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private var feeds: Map<String, NewReyes.Feed> = mapOf()
    private var broadcastName: String? = null

    private val chatViewModel: ChatViewModel by viewModels { viewModelFactory }
    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { viewModelFactory }

    private var isMe = false
    private val args by navArgs<StagePagerFragmentArgs>()
    @VisibleForTesting var stageIsLive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcasterUsername = args.broadcasterUsername()
        retainInstance = true

        launch {
            connectStage()
        }
    }

    private var connectStageJob: Job? = null

    fun connectStage() {
        if (connectStageJob == null) {
            connectStageJob = launch {
                val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
                connectStreams(userDetails.username)
                launch(dispatchConfig.main) {
                    connectMessages(userDetails.stageId)
                    connectFriendsWatching(userDetails.stageId)
                }
            }
        }
    }

    fun disconnectStage() {
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
        super.onDestroy()
    }

    private var viewJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inflate the layout for this fragment
        binding = FragmentStageBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        view.setOnClickListener { toggleAppBarVisibility() }
        profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            binding.userProfile = userProfile
            binding.shareButton?.setOnClickListener {
                val sharerId = followManager.currentUserDetails()?.caid
                startActivity(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
            }
            binding.showIsOverTextView.formatUsernameAsHtml(picasso, getString(R.string.broadcaster_show_is_over, userProfile.username))
        })
        viewJob = launch {
            launch(dispatchConfig.main) {
                val userDetails = followManager.userDetails(broadcasterUsername)
                if (userDetails != null) {
                    profileViewModel.load(userDetails.caid)

                    val colorRes = when {
                        followManager.isFollowing(userDetails.caid) -> R.color.caffeine_blue
                        else -> R.color.white
                    }
                    val fontColor = context?.getHexColor(colorRes)
                    val string = getString(R.string.say_something_to_user, broadcasterUsername, fontColor)
                    val html = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY, null, null) as Spannable
                    binding.saySomethingTextView?.text = html
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
        } else {
            viewsToToggle.plus(viewsForLiveOnly).forEach {
                it.isInvisible = true
            }
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
            binding.saySomethingTextView?.isVisible = messages.all { it.type == Message.Type.dummy }
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
            val action = StagePagerFragmentDirections.actionStagePagerFragmentToFriendsWatchingFragment(stageIdentifier)
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
                profileViewModel.follow(userDetails.caid).observe(viewLifecycleOwner, Observer { result ->
                    when(result) {
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
                })
            }
        }
        binding.backToLobbyButton.setOnClickListener {
            findNavController().popBackStack(R.id.lobbySwipeFragment, false)
        }
    }

    override fun sendDigitalItemWithMessage(message: String?) {
        val fragmentManager = fragmentManager ?: return
        val fragment = DICatalogFragment()
        val action = StagePagerFragmentDirections.actionStagePagerFragmentToDigitalItemListDialogFragment(broadcasterUsername, message)
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
        val action = StagePagerFragmentDirections.actionStagePagerFragmentToSendMessageFragment(message, !isMe)
        fragment.arguments = action.arguments
        fragment.show(fragmentManager, "sendMessage")
        fragment.setTargetFragment(this, SEND_MESSAGE)
    }

    override fun digitalItemSelected(digitalItem: DigitalItem, message: String?) {
        val fm = fragmentManager ?: return
        launch {
            val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
            val fragment = SendDigitalItemFragment()
            val action = StagePagerFragmentDirections.actionStagePagerFragmentToSendDigitalItemFragment(digitalItem.id, userDetails.caid, message)
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

}

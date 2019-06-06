package tv.caffeine.app.stage

import android.animation.LayoutTransition
import android.os.Bundle
import android.text.Spannable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.VisibleForTesting
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.FragmentStageBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.blink
import tv.caffeine.app.util.getHexColor
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject
import kotlin.collections.set

private const val PICK_DIGITAL_ITEM = 0
private const val SEND_MESSAGE = 1

class StageFragment @Inject constructor(
    private val factory: NewReyesController.Factory,
    private val eglBase: EglBase,
    private val followManager: FollowManager,
    private val picasso: Picasso,
    private val clock: Clock
) : CaffeineFragment(R.layout.fragment_stage), DICatalogFragment.Callback, SendMessageFragment.Callback {

    @Inject lateinit var chatMessageAdapter: ChatMessageAdapter
    @Inject lateinit var friendsWatchingAdapter: FriendsWatchingAdapter

    @VisibleForTesting lateinit var binding: FragmentStageBinding
    private lateinit var broadcasterUsername: String
    private lateinit var frameListener: EglRenderer.FrameListener
    private val canSwipe: Boolean by lazy { args.canSwipe }
    private val renderers: MutableMap<NewReyes.Feed.Role, SurfaceViewRenderer> = mutableMapOf()
    private val loadingIndicators: MutableMap<NewReyes.Feed.Role, ProgressBar> = mutableMapOf()
    private var newReyesController: NewReyesController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private var feeds: Map<String, NewReyes.Feed> = mapOf()

    private val chatViewModel: ChatViewModel by viewModels { viewModelFactory }
    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { viewModelFactory }

    @VisibleForTesting
    var feedQuality: NewReyes.Quality = NewReyes.Quality.GOOD

    private var isMe = false
    private val args by navArgs<StageFragmentArgs>()
    private var shouldShowOverlayOnProfileLoaded = true
    @VisibleForTesting var stageIsLive = false
    var swipeButtonOnClickListener: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcasterUsername = args.broadcastUsername
        retainInstance = true
    }

    private var connectStageJob: Job? = null

    fun connectStage() {
        if (connectStageJob == null) {
            loadingIndicators[NewReyes.Feed.Role.primary]?.isVisible = true
            connectStageJob = launch {
                val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
                connectStreams(userDetails.username)
                launch(dispatchConfig.main) {
                    connectMessages(userDetails.stageId)
                    connectFriendsWatching(userDetails.stageId)
                }
            }
        }
        if (viewJob == null) {
            viewJob = launch {
                do {
                    profileViewModel.forceLoad(broadcasterUsername)
                    delay(5000L)
                } while (isActive)
            }
        }
    }

    fun disconnectStage() {
        connectStageJob?.cancel()
        connectStageJob = null
        viewJob?.cancel()
        viewJob = null
        disconnectStreams()
        chatViewModel.disconnect()
        friendsWatchingViewModel.disconnect()
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

        view.setOnClickListener { toggleOverlayVisibility() }
        (view as ViewGroup).apply {
            layoutTransition = LayoutTransition()
            layoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING)
        }
        profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            binding.userProfile = userProfile
            binding.shareButton?.setOnClickListener {
                val sharerId = followManager.currentUserDetails()?.caid
                startActivity(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
            }
            binding.followButton.setOnClickListener {
                profileViewModel.follow(userProfile.caid).observe(viewLifecycleOwner, Observer { result ->
                    when (result) {
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
            binding.showIsOverTextView.formatUsernameAsHtml(picasso, getString(R.string.broadcaster_show_is_over, userProfile.username))
            binding.saySomethingTextView?.text = if (userProfile.isMe) {
                getString(R.string.messages_will_appear_here)
            } else {
                saySomethingToBroadcasterText(userProfile)
            }
            binding.avatarImageView.setOnClickListener {
                findNavController().safeNavigate(MainNavDirections.actionGlobalProfileFragment(userProfile.caid))
            }
            binding.moreButton.setOnClickListener {
                findNavController().navigateToReportOrIgnoreDialog(userProfile.caid, userProfile.username, true)
            }

            isMe = userProfile.isMe
            updateViewsOnMyStageVisibility()
            updateBroadcastOnlineState(userProfile.isLive)
            if (shouldShowOverlayOnProfileLoaded) {
                shouldShowOverlayOnProfileLoaded = false
                toggleOverlayVisibility(!userProfile.isLive, false) // hide on the first frame instead of a timeout if live
            }
        })
        val navController = findNavController()
        binding.stageToolbar.setupWithNavController(navController, null)
        binding.messagesRecyclerView?.adapter = chatMessageAdapter
        chatMessageAdapter.callback = object : ChatMessageAdapter.Callback {
            override fun replyClicked(message: Message) {
                val string = getString(R.string.username_prepopulated_reply, message.publisher.username)
                openSendMessage(string)
            }

            override fun upvoteClicked(message: Message) {
                chatViewModel.endorseMessage(message)
            }
        }
        binding.noNetworkDataBlinkingImageView.blink()
        initSurfaceViewRenderer()
        configureButtons()
    }

    private fun saySomethingToBroadcasterText(userProfile: UserProfile): Spannable {
        val colorRes = when {
            userProfile.isFollowed -> R.color.caffeine_blue
            else -> R.color.white
        }
        val fontColor = context?.getHexColor(colorRes)
        val string = getString(R.string.say_something_to_user, broadcasterUsername, fontColor)
        return HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY, null, null) as Spannable
    }

    override fun onDestroyView() {
        deinitSurfaceViewRenderers()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        val isStreamConnected = newReyesController != null // true after screen rotation
        shouldShowOverlayOnProfileLoaded = !isStreamConnected
        connectStage()
    }

    private fun isChangingConfigurations() = activity?.isChangingConfigurations != false

    override fun onPause() {
        if (!isChangingConfigurations()) disconnectStage()
        super.onPause()
    }

    private fun initSurfaceViewRenderer() {
        renderers[NewReyes.Feed.Role.primary] = binding.primaryViewRenderer
        renderers[NewReyes.Feed.Role.secondary] = binding.secondaryViewRenderer
        binding.secondaryViewRenderer.setZOrderMediaOverlay(true)
        loadingIndicators[NewReyes.Feed.Role.primary] = binding.primaryLoadingIndicator
        // TODO: clean up the secondary spinner if the design is approved.
        // loadingIndicators[NewReyes.Feed.Role.secondary] = binding.secondaryLoadingIndicator
        renderers.forEach { entry ->
            val key = entry.key
            val renderer = entry.value
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            renderer.setEnableHardwareScaler(true)
            feeds.values.firstOrNull { it.role == key }?.let { feed ->
                configureRenderer(renderer, feed, videoTracks[feed.stream.id])
            }
            renderer.setOnClickListener { toggleOverlayVisibility() }
        }
    }

    private var overlayVisibilityJob: Job? = null

    private fun toggleOverlayVisibility(shouldAutoHideAfterTimeout: Boolean = true, shouldIncludeAppBar: Boolean = true) {
        overlayVisibilityJob?.cancel()
        if (!binding.stageAppbar.isVisible || !binding.liveIndicatorAndAvatarContainer.isVisible) {
            showOverlays(shouldIncludeAppBar)
            if (shouldAutoHideAfterTimeout) {
                overlayVisibilityJob = launch {
                    delay(3000)
                    hideOverlays()
                }
            }
        } else {
            hideOverlays()
        }
    }

    @VisibleForTesting
    fun showOverlays(shouldIncludeAppBar: Boolean = true) = setOverlayVisible(true, shouldIncludeAppBar)

    @VisibleForTesting
    fun hideOverlays() = setOverlayVisible(false)

    @VisibleForTesting
    fun setOverlayVisible(visible: Boolean, shouldIncludeAppBar: Boolean = true) {
        val viewsToToggle = if (shouldIncludeAppBar) {
            listOf(binding.stageAppbar, binding.liveIndicatorAndAvatarContainer)
        } else {
            listOf(binding.liveIndicatorAndAvatarContainer)
        }

        val viewsForLiveOnly = listOf(binding.gameLogoImageView, binding.liveIndicatorTextView, binding.weakConnectionContainer)

        if (visible) {
            viewsToToggle.forEach {
                it.isVisible = true
            }

            // hide blinker when showing overlay
            if (feedQuality == NewReyes.Quality.POOR) {
                showPoorConnectionAnimation(false)
            }

            // views for live only
            binding.gameLogoImageView.isVisible = stageIsLive
            binding.avatarUsernameContainer.isVisible = stageIsLive
            binding.liveIndicatorTextView.isVisible = stageIsLive
            binding.weakConnectionContainer.isVisible = feedQuality != NewReyes.Quality.GOOD
        } else {
            viewsToToggle.plus(viewsForLiveOnly).forEach {
                it.isVisible = false
            }
            // show blinker when hiding overlay
            if (feedQuality == NewReyes.Quality.POOR) {
                showPoorConnectionAnimation(true)
            }
        }
    }

    @VisibleForTesting
    fun showPoorConnectionAnimation(isVisible: Boolean) {
        binding.noNetworkDataBlinkingImageView.isVisible = isVisible
    }

    private fun updateViewsOnMyStageVisibility() {
        listOf(
            binding.giftButton,
            binding.friendsWatchingButton,
            binding.avatarImageView,
            binding.usernameTextView,
            binding.followButton,
            binding.broadcastTitleTextView
        ).forEach {
            it?.isVisible = !isMe
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
                    .resizeDimen(R.dimen.avatar_friends_watching, R.dimen.avatar_friends_watching)
                    .placeholder(R.drawable.ic_profile)
                    .transform(profileAvatarTransform)
                    .into(binding.friendsWatchingButton)
        }
    }

    @VisibleForTesting
    fun updateBroadcastOnlineState(broadcastIsOnline: Boolean) {
        stageIsLive = broadcastIsOnline
        if (!broadcastIsOnline) {
            loadingIndicators[NewReyes.Feed.Role.primary]?.isVisible = false
        }
        binding.showIsOverTextView.isVisible = !broadcastIsOnline
        binding.backToLobbyButton.isVisible = !broadcastIsOnline
    }

    private fun configureRenderer(renderer: SurfaceViewRenderer, feed: NewReyes.Feed?, videoTrack: VideoTrack?) {
        val hasVideo = videoTrack != null && (feed?.capabilities?.video ?: false)
        renderer.isVisible = hasVideo
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
        manageFeedQuality(controller)
        manageStateChange(controller)
        manageConnections(controller)
        manageErrors(controller)
    }

    private fun manageFeeds(controller: NewReyesController) = launch {
        controller.feedChannel.consumeEach { mapOfFeeds ->
            feeds = mapOfFeeds
            if (feeds.isEmpty()) {
                // Reyes should be the source of truth for stage liveness. Roadhog can be inaccurate.
                updateBroadcastOnlineState(false)
            }
            val activeRoles = feeds.values.filter { it.capabilities.video }.map { it.role }.toList()
            renderers[NewReyes.Feed.Role.primary]?.isVisible = NewReyes.Feed.Role.primary in activeRoles
            renderers[NewReyes.Feed.Role.secondary]?.isVisible = NewReyes.Feed.Role.secondary in activeRoles
        }
    }

    private fun manageFeedQuality(controller: NewReyesController) = launch {
        controller.feedQualityChannel.consumeEach {
            feedQuality = it
            showPoorConnectionAnimation(it == NewReyes.Quality.POOR)
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
            val videoTrack = feedInfo.connectionInfo.videoTrack
            videoTrack?.let { videoTracks[feedInfo.streamId] = it }
            renderers[feedInfo.role]?.let { renderer ->
                configureRenderer(renderer, feedInfo.feed, videoTrack)
                if (feedInfo.role == NewReyes.Feed.Role.primary) {
                    frameListener = EglRenderer.FrameListener {
                        launch(Dispatchers.Main) {
                            renderer.removeFrameListener(frameListener)
                            loadingIndicators[feedInfo.role]?.isVisible = false
                            hideOverlays()
                        }
                    }
                    renderer.addFrameListener(frameListener, 1.0f)
                }
            }
        }
    }

    private fun manageErrors(controller: NewReyesController) = launch {
        controller.errorChannel.consumeEach { error ->
            when (error) {
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
            val fragment = FriendsWatchingFragment(friendsWatchingAdapter)
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

    @VisibleForTesting fun configureButtons() {
        binding.chatButton?.setOnClickListener { openSendMessage() }
        binding.giftButton?.setOnClickListener {
            sendDigitalItemWithMessage(null)
        }
        binding.backToLobbyButton.setOnClickListener {
            findNavController().popBackStack(R.id.lobbySwipeFragment, false)
        }
        binding.stageToolbar.apply { setTitleTextColor(context.getColor(R.color.transparent)) }
        binding.swipeButton.isVisible = canSwipe
        binding.swipeButton.setOnClickListener(swipeButtonOnClickListener)
    }

    override fun sendDigitalItemWithMessage(message: String?) {
        val fragmentManager = fragmentManager ?: return
        val fragment = DICatalogFragment(picasso)
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
            val fragment = SendDigitalItemFragment(picasso)
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

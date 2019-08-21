package tv.caffeine.app.stage

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.VisibleForTesting
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
import org.webrtc.EglRenderer
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.databinding.FragmentStageBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.PulseAnimator
import tv.caffeine.app.util.inTransaction
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import tv.caffeine.app.util.transformToClassicUI
import tv.caffeine.app.webrtc.SurfaceViewRendererTuner
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.set

class StageFragment @Inject constructor(
    private val factory: NewReyesController.Factory,
    private val surfaceViewRendererTuner: SurfaceViewRendererTuner,
    private val followManager: FollowManager,
    private val picasso: Picasso,
    @VisibleForTesting
    val releaseDesignConfig: ReleaseDesignConfig
) : CaffeineFragment(R.layout.fragment_stage), StageBroadcastDetailsPagerFragment.Callback {

    @Inject lateinit var stageBroadcastDetailsPagerFragmentProvider: Provider<StageBroadcastDetailsPagerFragment>

    @VisibleForTesting
    lateinit var binding: FragmentStageBinding
    private lateinit var broadcasterUsername: String
    private lateinit var frameListener: EglRenderer.FrameListener
    private lateinit var poorConnectionPulseAnimator: PulseAnimator
    private val canSwipe: Boolean by lazy { args.canSwipe }
    private val renderers: MutableMap<NewReyes.Feed.Role, SurfaceViewRenderer> = mutableMapOf()
    private val loadingIndicators: MutableMap<NewReyes.Feed.Role, ProgressBar> = mutableMapOf()
    private var newReyesController: NewReyesController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private var feeds: Map<String, NewReyes.Feed> = mapOf()

    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { viewModelFactory }

    @VisibleForTesting
    var feedQuality: FeedQuality = FeedQuality.GOOD
    private var overlayIsVisible = false

    private var isMe = false
    private val args by navArgs<StageFragmentArgs>()
    private var shouldShowOverlayOnProfileLoaded = true
    @VisibleForTesting
    var stageIsLive = false
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

        var isReleaseDesign = releaseDesignConfig.isReleaseDesignActive()
        binding.isReleaseDesign = isReleaseDesign

        if (!isReleaseDesign) {
            binding.avatarUsernameContainer.transformToClassicUI()
        }

        view.setOnClickListener { toggleOverlayVisibility() }
        (view as ViewGroup).apply {
            layoutTransition = LayoutTransition()
            layoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING)
        }
        profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            binding.userProfile = userProfile
            binding.showIsOverTextView.formatUsernameAsHtml(
                picasso,
                getString(R.string.broadcaster_show_is_over, userProfile.username)
            )
            binding.avatarImageView.setOnClickListener {
                if (isReleaseDesign) {
                    updateBottomFragment(BottomContainerType.PROFILE, userProfile.caid)
                } else {
                    findNavController().safeNavigate(MainNavDirections.actionGlobalProfileFragment(userProfile.caid))
                }
            }
            binding.moreButton.setOnClickListener {
                findNavController().navigateToReportOrIgnoreDialog(userProfile.caid, userProfile.username, true)
            }

            if (isReleaseDesign) {
                binding.stageToolbar.apply {
                    if (menu.findItem(R.id.overflow_menu_item) != null) return@apply
                    inflateMenu(R.menu.overflow_menu)
                    menu.findItem(R.id.overflow_menu_item).setOnMenuItemClickListener {
                        if (it.itemId == R.id.overflow_menu_item) {
                            findNavController().navigateToReportOrIgnoreDialog(
                                userProfile.caid, userProfile.username, true
                            )
                        }
                        true
                    }
                }
            }

            // TODO: extract to VM
            val followListener = View.OnClickListener {
                if (userProfile.isFollowed) {
                    profileViewModel.unfollow(userProfile.caid)
                } else {
                    profileViewModel.follow(userProfile.caid).observe(this, Observer { result ->
                        when (result) {
                            is CaffeineEmptyResult.Error -> {
                                if (result.error.isMustVerifyEmailError()) {
                                    val fragment =
                                        AlertDialogFragment.withMessage(R.string.verify_email_to_follow_more_users)
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

            binding.followButtonText.setOnClickListener(followListener)
            binding.followButtonImage.setOnClickListener(followListener)

            isMe = userProfile.isMe
            updateViewsOnMyStageVisibility()
            updateBroadcastOnlineState(userProfile.isLive)
            if (shouldShowOverlayOnProfileLoaded) {
                shouldShowOverlayOnProfileLoaded = false
                toggleOverlayVisibility(
                    !userProfile.isLive,
                    false
                ) // hide on the first frame instead of a timeout if live
            }
        })
        val navController = findNavController()
        binding.stageToolbar.setupWithNavController(navController, null)
        poorConnectionPulseAnimator = PulseAnimator(binding.poorConnectionPulseImageView)
        initSurfaceViewRenderer()
        configureButtons()

        updateBottomFragment(BottomContainerType.CHAT)
    }

    @VisibleForTesting
    fun updateBottomFragment(bottomContainerType: BottomContainerType, caid: String = "") {
        binding.bottomFragmentContainer?.let {
            // TODO: Add Bio section fragment here
            val fragment = when (bottomContainerType) {
                BottomContainerType.CHAT -> {
                    val isRelease = releaseDesignConfig.isReleaseDesignActive()
                    ChatFragment.newInstance(broadcasterUsername, isRelease)
                }
                BottomContainerType.PROFILE -> {
                    stageBroadcastDetailsPagerFragmentProvider.get().apply {
                        arguments = StageBroadcastDetailsPagerFragmentArgs(broadcasterUsername, caid).toBundle()
                    }
                }
            }
            childFragmentManager.inTransaction {
                replace(R.id.bottom_fragment_container, fragment)
            }
        }
    }

    override fun returnToChat() {
        updateBottomFragment(BottomContainerType.CHAT)
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
            surfaceViewRendererTuner.configure(renderer)
            feeds.values.firstOrNull { it.role == key }?.let { feed ->
                configureRenderer(renderer, feed, videoTracks[feed.stream.id])
            }
            renderer.setOnClickListener { toggleOverlayVisibility() }
        }
    }

    private var overlayVisibilityJob: Job? = null

    private fun toggleOverlayVisibility(
        shouldAutoHideAfterTimeout: Boolean = true,
        shouldIncludeAppBar: Boolean = true
    ) {
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
        overlayIsVisible = visible
        updatePoorConnectionAnimation()

        val viewsToToggle = if (shouldIncludeAppBar) {
            listOf(binding.stageAppbar, binding.liveIndicatorAndAvatarContainer)
        } else {
            listOf(binding.liveIndicatorAndAvatarContainer)
        }

        if (visible) {
            viewsToToggle.forEach {
                it.isVisible = true
            }

            // views for live only
            manageConnectionQualityDependentViews(stageIsLive)
        } else {
            viewsToToggle.forEach {
                it.isVisible = false
            }
        }
    }

    private fun manageConnectionQualityDependentViews(stageIsLive: Boolean) {
        when (feedQuality) {
            FeedQuality.GOOD -> {
                binding.gameLogoImageView.isVisible = stageIsLive
                binding.liveIndicatorTextView.isVisible = stageIsLive
                binding.avatarUsernameContainer.isVisible = true
                binding.weakConnectionContainer.isVisible = false
            }
            FeedQuality.POOR -> {
                binding.gameLogoImageView.isVisible = stageIsLive
                binding.liveIndicatorTextView.isVisible = stageIsLive
                binding.avatarUsernameContainer.isVisible = true
                binding.weakConnectionContainer.isVisible = true
            }
            else -> {
                binding.gameLogoImageView.isVisible = false
                binding.liveIndicatorTextView.isVisible = false
                binding.avatarUsernameContainer.isVisible = false
                binding.weakConnectionContainer.isVisible = false
            }
        }
    }

    @VisibleForTesting
    fun updatePoorConnectionAnimation() {
        if (!overlayIsVisible && feedQuality == FeedQuality.POOR) {
            poorConnectionPulseAnimator.startPulse()
        } else {
            poorConnectionPulseAnimator.stopPulse()
        }
    }

    fun updateBadConnectionOverlay() {
        binding.badConnectionContainer.isVisible = feedQuality == FeedQuality.BAD
    }

    private fun updateViewsOnMyStageVisibility() {
        listOf(
            binding.avatarImageView,
            binding.usernameTextView,
            binding.followButtonText,
            binding.followButtonImage,
            binding.broadcastTitleTextView
        ).forEach {
            it?.isVisible = !isMe
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
        val controller = factory.create(username, false)
        controller.connect()
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
            updatePoorConnectionAnimation()
            updateBadConnectionOverlay()
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

    private fun disconnectStreams() {
        newReyesController?.close()
        newReyesController = null
        videoTracks.clear()
    }

    @VisibleForTesting
    fun configureButtons() {
        binding.backToLobbyButton.setOnClickListener {
            findNavController().popBackStack(R.id.lobbySwipeFragment, false)
        }
        binding.stageToolbar.apply { setTitleTextColor(context.getColor(R.color.transparent)) }
        binding.swipeButton.isVisible = canSwipe
        binding.swipeButton.setOnClickListener(swipeButtonOnClickListener)
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

enum class BottomContainerType {
    PROFILE,
    CHAT
}

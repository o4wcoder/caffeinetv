package tv.caffeine.app.stage

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
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
import org.webrtc.EglRenderer
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.databinding.FragmentStageBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.PulseAnimator
import tv.caffeine.app.util.fadeOutLoadingIndicator
import tv.caffeine.app.util.inTransaction
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
    private val clock: Clock,
    @VisibleForTesting
    val releaseDesignConfig: ReleaseDesignConfig
) : CaffeineFragment(R.layout.fragment_stage), ChatActionCallback {

    @Inject lateinit var stageBroadcastProfilePagerFragmentProvider: Provider<StageBroadcastProfilePagerFragment>

    @VisibleForTesting
    lateinit var binding: FragmentStageBinding
    private lateinit var broadcasterUsername: String
    private lateinit var frameListener: EglRenderer.FrameListener
    private lateinit var poorConnectionPulseAnimator: PulseAnimator
    private val canSwipe: Boolean by lazy { args.canSwipe }
    private val renderers: MutableMap<NewReyes.Feed.Role, SurfaceViewRenderer> = mutableMapOf()
    private val loadingIndicators: MutableMap<NewReyes.Feed.Role, View> = mutableMapOf()
    private var newReyesController: NewReyesController? = null
    private val videoTracks: MutableMap<String, VideoTrack> = mutableMapOf()
    private var feeds: Map<String, NewReyes.Feed> = mapOf()
    private lateinit var stageId: String
    private var hasFriendsWatching: Boolean = false

    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { viewModelFactory }
    @VisibleForTesting
    lateinit var stageViewModel: StageViewModel

    private val args by navArgs<StageFragmentArgs>()
    @VisibleForTesting
    var haveSetupBottomSection = false
    private var isProfileShowing = false

    @VisibleForTesting
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
                stageId = userDetails.stageId
                connectStreams(userDetails.username)
                friendsWatchingViewModel.load(stageId)
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

    override fun onDetach() {
        // Since we are retaining the instance of the fragment, need to reset this flag to setup
        // the bottom section on configuration change
        haveSetupBottomSection = false
        super.onDetach()
    }

    override fun onDestroy() {
        disconnectStage()
        super.onDestroy()
    }

    private var viewJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentStageBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        stageViewModel = StageViewModel(releaseDesignConfig, ::onAvatarButtonClick)
        binding.viewModel = stageViewModel

        observeFollowEvents()

        stageViewModel.showPoorConnectionAnimation.observe(viewLifecycleOwner, Observer {
            if (it) poorConnectionPulseAnimator.startPulse() else poorConnectionPulseAnimator.stopPulse()
        })

        // TODO: When release design goes live, remove this and just update styles
        if (releaseDesignConfig.isReleaseDesignActive()) {
            TextViewCompat.setTextAppearance(binding.broadcastTitleTextView, R.style.BroadcastTitle_Night_Release)
            TextViewCompat.setTextAppearance(binding.usernameTextView, R.style.BroadcasterUsername_Night_Release)
        }

        configureFriendsWatchingIndicator()

        if (!releaseDesignConfig.isReleaseDesignActive()) {
            binding.avatarUsernameContainer.transformToClassicUI()
            binding.liveSwipeContainer.transformToClassicUI()
        }

        (view as ViewGroup).apply {
            layoutTransition = LayoutTransition()
            layoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING)
        }

        profileViewModel.isFollowing.observe(viewLifecycleOwner, Observer {
            binding.releaseIsFollowing = it
        })

        profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            binding.userProfile = userProfile
            binding.showIsOverTextView.formatUsernameAsHtml(
                picasso,
                getString(R.string.broadcaster_show_is_over, userProfile.username)
            )
            binding.moreButton.setOnClickListener {
                findNavController().navigateToReportOrIgnoreDialog(userProfile.caid, userProfile.username, true)
            }

            stageViewModel.updateIsFollowed(userProfile.isFollowed)
            updateAvatarImageViewBackground()
            binding.usernameTextView.setTextColor(ContextCompat.getColor(binding.usernameTextView.context, stageViewModel.usernameTextColor))

            if (releaseDesignConfig.isReleaseDesignActive()) {
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
            stageViewModel.updateIsMe(userProfile.isMe)
            setupFollowClickListener(userProfile)
            updateBottomFragmentForOnlineStatus(userProfile)
            updateBroadcastOnlineState(userProfile.isLive)
            setupOverlays(userProfile.isLive)
        })
        val navController = findNavController()
        binding.stageToolbar.setupWithNavController(navController, null)
        poorConnectionPulseAnimator = PulseAnimator(binding.poorConnectionPulseImageView)
        initSurfaceViewRenderer()
        configureButtons()
    }

    @VisibleForTesting
    fun setupOverlays(isLive: Boolean) {
        if (isLive) {
            binding.root.setOnClickListener { toggleOverlayVisibility() }
            // Only want to setup the overlay toggle once or the overlays will flash on and off
            if (stageViewModel.shouldShowInitialOverlays) {
                stageViewModel.shouldShowInitialOverlays = false
                // hide app bar on the first frame instead of a timeout if live
                toggleOverlayVisibility(
                    true,
                    false) // hide on the first frame instead of a timeout if live
            }
        } else {
            // Always show overlays on offline stage
            showOverlays(true)
        }
    }

    private fun setupFollowClickListener(userProfile: UserProfile) {
        // TODO: extract to VM
        val followListener = View.OnClickListener {
            if (userProfile.isFollowed) {
                profileViewModel.unfollow(userProfile.caid)
            } else {
                profileViewModel.follow(userProfile.caid)
            }
        }
        binding.followButtonText.setOnClickListener(followListener)
        binding.followButtonImage.setOnClickListener(followListener)
    }

    private fun onAvatarButtonClick() {
        binding.userProfile?.let {
            if (releaseDesignConfig.isReleaseDesignActive()) {
                isProfileShowing = !isProfileShowing
                onProfileToggleButtonClick(isProfileShowing, it.caid)
            } else {
                findNavController().safeNavigate(
                    MainNavDirections.actionGlobalProfileFragment(it.caid)
                )
            }
        }
    }

    @VisibleForTesting
    fun onProfileToggleButtonClick(isProfileShowing: Boolean, caid: CAID) {
        if (isProfileShowing) {
            overlayVisibilityJob?.cancel()
            updateBottomFragment(BottomContainerType.PROFILE, caid)
        } else {
            hideOverlays()
            updateBottomFragment(BottomContainerType.CHAT, caid)
        }
    }

    @VisibleForTesting
    fun updateBottomFragment(bottomContainerType: BottomContainerType, caid: String = "", chatAction: ChatAction? = null) {
        stageViewModel.updateIsViewProfile(bottomContainerType == BottomContainerType.PROFILE)
        updateAvatarImageViewBackground()
        binding.bottomFragmentContainer?.let {
            val fragment = when (bottomContainerType) {
                BottomContainerType.CHAT -> {
                    ChatFragment.newInstance(broadcasterUsername, releaseDesignConfig.isReleaseDesignActive(), chatAction)
                }
                BottomContainerType.PROFILE -> {
                    stageBroadcastProfilePagerFragmentProvider.get().apply {
                        arguments = StageBroadcastProfilePagerFragmentArgs(
                            broadcasterUsername,
                            caid,
                            binding.userProfile?.getFollowersString(),
                            binding.userProfile?.getFollowingString()).toBundle()
                    }
                }
            }
            childFragmentManager.inTransaction {
                replace(R.id.bottom_fragment_container, fragment)
            }
        }
    }

    @VisibleForTesting
    fun updateBottomFragmentForOnlineStatus(userProfile: UserProfile) {
        // If the user is offline, start with the profile section on the bottom, otherwise show chat. Only do this once
        if (!haveSetupBottomSection) {
            haveSetupBottomSection = true
            if (releaseDesignConfig.isReleaseDesignActive() && !userProfile.isLive) {
                isProfileShowing = true
                updateBottomFragment(BottomContainerType.PROFILE, userProfile.caid)
            } else {
                updateBottomFragment(BottomContainerType.CHAT)
            }
        }
    }

    override fun onDestroyView() {
        deinitSurfaceViewRenderers()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
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
                setFeedContentRating()
            }
            renderer.setOnClickListener { toggleOverlayVisibility() }
        }
    }

    private fun setFeedContentRating() {
        feeds.values.firstOrNull()?.let { stageViewModel.contentRating = it.contentRating }
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
            if (isProfileShowing) {
                isProfileShowing = !isProfileShowing
                binding.userProfile?.let { updateBottomFragment(BottomContainerType.CHAT, it.caid) }
            }
        }
    }

    @VisibleForTesting
    fun showOverlays(shouldIncludeAppBar: Boolean = true) = setOverlayVisible(true, shouldIncludeAppBar)

    @VisibleForTesting
    fun hideOverlays() = setOverlayVisible(false)

    @VisibleForTesting
    fun setOverlayVisible(visible: Boolean, shouldIncludeAppBar: Boolean = true) {
        stageViewModel.updateOverlayIsVisible(visible, shouldIncludeAppBar)

        updateAvatarImageViewBackground()

        binding.stageAppbar.isVisible = stageViewModel.getAppBarVisibility()
        binding.avatarUsernameContainer.isVisible = stageViewModel.getAvatarUsernameContainerVisibility()
        binding.liveIndicatorAndAvatarContainer.isVisible = stageViewModel.getLiveIndicatorAndAvatarContainerVisibility()
        binding.gameLogoImageView.isVisible = stageViewModel.getGameLogoVisibility()
        binding.avatarOverlapLiveBadge.isInvisible = !stageViewModel.getAvatarOverlapLiveBadgeVisibility()
        binding.classicLiveIndicatorTextView.isInvisible = !stageViewModel.getClassicLiveIndicatorTextViewVisibility()
        binding.weakConnectionContainer.isVisible = stageViewModel.getWeakConnnectionContainerVisibility()
        binding.swipeButton.isVisible = stageViewModel.getSwipeButtonVisibility()
    }

    fun updateAvatarImageViewBackground() {
        binding.avatarImageView.background = ContextCompat.getDrawable(binding.avatarImageView.context, stageViewModel.avatarImageBackground)
    }

    @VisibleForTesting // TODO: view model
    fun updateBroadcastOnlineState(broadcastIsOnline: Boolean) {
        stageViewModel.updateStageIsLive(broadcastIsOnline)
        if (!broadcastIsOnline) {
            loadingIndicators[NewReyes.Feed.Role.primary]?.fadeOutLoadingIndicator()
        }

        if (releaseDesignConfig.isReleaseDesignActive()) {
            stageViewModel.isProfileOverlayVisible = !broadcastIsOnline
            binding.showIsOverTextView.isVisible = false
            binding.backToLobbyButton.isVisible = false
        } else {
            stageViewModel.isProfileOverlayVisible = false
            binding.showIsOverTextView.isVisible = !broadcastIsOnline
            binding.backToLobbyButton.isVisible = !broadcastIsOnline
        }
    }

    private fun configureFriendsWatchingIndicator() {
        friendsWatchingViewModel.friendsWatching.observe(viewLifecycleOwner, Observer {
            hasFriendsWatching = it.isNotEmpty()
            binding.avatarOverlapLiveBadge.stageFollowers = it
        })

        binding.avatarOverlapLiveBadge.setOnClickListener {
            if (hasFriendsWatching) {
                val action =
                    StagePagerFragmentDirections.actionStagePagerFragmentToFriendsWatchingFragment(
                        stageId
                    )
                findNavController().safeNavigate(action)
            }
        }
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

            setFeedContentRating()
        }
    }

    private fun manageFeedQuality(controller: NewReyesController) = launch {
        controller.feedQualityChannel.consumeEach {
            stageViewModel.updateFeedQuality(it)
            binding.badConnectionContainer.isVisible = stageViewModel.getBadConnectionOverlayVisibility()
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
                            loadingIndicators[feedInfo.role]?.fadeOutLoadingIndicator()
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
        binding.swipeButton.isVisible = canSwipe // TODO: view model
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

    override fun processChatAction(type: ChatAction) {

        updateBottomFragment(BottomContainerType.CHAT, chatAction = type)
        hideOverlays()
        isProfileShowing = false

        if (type == ChatAction.SHARE) shareBroadcast()
    }

    private fun shareBroadcast() {
        binding.userProfile?.let {
            val sharerId = followManager.currentUserDetails()?.caid
            startActivity(
                StageShareIntentBuilder(
                    it,
                    sharerId,
                    resources,
                    clock
                ).build()
            )
        }
    }
}

enum class BottomContainerType {
    PROFILE,
    CHAT
}
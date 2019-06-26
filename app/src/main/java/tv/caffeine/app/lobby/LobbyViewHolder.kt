package tv.caffeine.app.lobby

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import org.webrtc.EglRenderer
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.analytics.EventManager
import tv.caffeine.app.api.LobbyCardClickedEvent
import tv.caffeine.app.api.LobbyClickedEventData
import tv.caffeine.app.api.LobbyFollowClickedEvent
import tv.caffeine.app.api.LobbyImpressionEvent
import tv.caffeine.app.api.LobbyImpressionEventData
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.makeLobbyImpressionEventData
import tv.caffeine.app.databinding.CardListBinding
import tv.caffeine.app.databinding.LiveBroadcastCardBinding
import tv.caffeine.app.databinding.LiveBroadcastWithFriendsCardBinding
import tv.caffeine.app.databinding.LobbyAvatarCardBinding
import tv.caffeine.app.databinding.LobbyFollowPeopleCardBinding
import tv.caffeine.app.databinding.LobbyHeaderBinding
import tv.caffeine.app.databinding.LobbySubtitleBinding
import tv.caffeine.app.databinding.PreviousBroadcastCardBinding
import tv.caffeine.app.databinding.UpcomingButtonCardBinding
import tv.caffeine.app.di.ThemeFollowedLobby
import tv.caffeine.app.di.ThemeFollowedLobbyLight
import tv.caffeine.app.di.ThemeNotFollowedLobby
import tv.caffeine.app.di.ThemeNotFollowedLobbyLight
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.AutoPlayConfig
import tv.caffeine.app.stage.NewReyesController
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.webrtc.SurfaceViewRendererTuner

sealed class LobbyViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: LobbyItem) {
        itemView.tag = item.itemType
        configure(item)
    }
    protected abstract fun configure(item: LobbyItem)
    open fun recycle() = Unit
}

class AvatarCard(
    val binding: LobbyAvatarCardBinding
) : LobbyViewHolder(binding.root) {

    override fun configure(item: LobbyItem) {
        binding.username = (item as WelcomeCard).username
        itemView.setOnClickListener {
            val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToMyProfileFragment(true)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}

class FollowPeopleCard(
    val binding: LobbyFollowPeopleCardBinding
) : LobbyViewHolder(binding.root) {

    override fun configure(item: LobbyItem) {
        binding.displayMessage = (item as FollowPeople).displayMessage
    }
}

class HeaderCard(
    val binding: LobbyHeaderBinding
) : LobbyViewHolder(binding.root) {

    override fun configure(item: LobbyItem) {
        binding.viewModel = item as Header
    }
}

class SubtitleCard(
    val binding: LobbySubtitleBinding
) : LobbyViewHolder(binding.root) {

    override fun configure(item: LobbyItem) {
        binding.viewModel = item as Subtitle
    }
}

abstract class BroadcasterCard(
    view: View,
    protected val tags: Map<String, Lobby.Tag>,
    protected val content: Map<String, Lobby.Content>,
    val followManager: FollowManager,
    @ThemeFollowedLobby private val followedTheme: UserTheme,
    @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme,
    @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
    @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme,
    protected val picasso: Picasso,
    private val payloadId: String?,
    private val scope: CoroutineScope? = null,
    private val clock: Clock,
    protected val eventManager: EventManager
) : LobbyViewHolder(view) {
    protected val previewImageView: ImageView = view.findViewById(R.id.preview_image_view)
    private val avatarImageView: ImageView = view.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = view.findViewById(R.id.username_text_view)
    private val broadcastTitleTextView: TextView = view.findViewById(R.id.broadcast_title_text_view)
    private val tagTextView: TextView = view.findViewById(R.id.tag_text_view)
    private val followButton: TextView = view.findViewById(R.id.follow_button)
    private val pipImageView: ImageView? = itemView.findViewById(R.id.pip_image_view)

    protected open val cornerType: RoundedCornersTransformation.CornerType = RoundedCornersTransformation.CornerType.TOP
    protected open val isLight: Boolean = false
    private val roundedCornersTransformation by lazy { RoundedCornersTransformation(itemView.resources.getDimension(R.dimen.lobby_card_rounding_radius).toInt(), 0, cornerType) }

    protected var stageController: NewReyesController? = null

    override fun recycle() {
        stageController?.close()
        stageController = null
    }

    override fun configure(item: LobbyItem) {
        val singleCard = item as SingleCard
        val broadcast = singleCard.broadcaster.broadcast ?: singleCard.broadcaster.lastBroadcast ?: error("Unexpected lobby item state")
        picasso
                .load(broadcast.mainPreviewImageUrl)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.default_lobby_image)
                .transform(roundedCornersTransformation)
                .into(previewImageView)
        val isLive = singleCard.broadcaster.broadcast != null
        val followHandler = FollowManager.FollowHandler(null, object : FollowManager.Callback() {
            override fun follow(caid: CAID) {
                scope?.launch {
                    followManager.followUser(caid, object : FollowManager.FollowCompletedCallback {
                        override fun onUserFollowed() {
                            configureUser(singleCard.broadcaster.user, null)
                        }
                    })
                    getLobbyClickedEventData(singleCard)?.let { eventData ->
                        eventManager.sendEvent(LobbyFollowClickedEvent(eventData))
                    }
                }
            }

            override fun unfollow(caid: CAID) {
            }
        })
        configureUser(singleCard.broadcaster.user, followHandler)
        avatarImageView.setOnClickListener { viewProfile(item.broadcaster.user.caid) }
        usernameTextView.setOnClickListener { viewProfile(item.broadcaster.user.caid) }
        broadcastTitleTextView.text = broadcast.name
        val tag = tags[singleCard.broadcaster.tagId]
        tagTextView.isVisible = tag != null
        if (tag != null) {
            tagTextView.text = tag.name
            if (isLive) {
                tagTextView.setTextColor(Color.WHITE)
                tagTextView.backgroundTintList = ColorStateList.valueOf(tag.color.toColorInt())
            } else {
                tagTextView.setTextColor(tag.color.toColorInt())
            }
        }
        pipImageView?.apply {
            isVisible = broadcast.hasLiveHostedBroadcaster
            if (broadcast.hasLiveHostedBroadcaster) {
                picasso
                        .load(broadcast.pictureInPictureImageUrl)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.default_lobby_image)
                        .transform(roundedCornersTransformation)
                        .into(this)
            }
        }

        sendImpressionEventData(item)
    }

    private fun configureUser(user: User, followHandler: FollowManager.FollowHandler?) {
        user.configure(
            avatarImageView,
            usernameTextView,
            followButton,
            followManager,
            false,
            followHandler,
            R.dimen.avatar_size,
            if (isLight) followedThemeLight else followedTheme,
            if (isLight) notFollowedThemeLight else notFollowedTheme
        )
    }

    open fun viewProfile(caid: CAID) {
        val action = MainNavDirections.actionGlobalProfileFragment(caid)
        Navigation.findNavController(itemView).safeNavigate(action)
    }

    private fun Clock.seconds() = millis() / 1000

    @VisibleForTesting fun getLobbyClickedEventData(singleCard: SingleCard): LobbyClickedEventData? {
        if (payloadId == null) return null
        val caid = followManager.currentUserDetails()?.caid
        val stageId = singleCard.broadcaster.user.stageId
        val timestamp = clock.seconds()
        return LobbyClickedEventData(payloadId, caid, stageId, timestamp)
    }

    @VisibleForTesting
    fun getLobbyImpressionEventData(singleCard: SingleCard): LobbyImpressionEventData? =
        payloadId?.let { singleCard.broadcaster.makeLobbyImpressionEventData(it) }

    private fun sendImpressionEventData(lobbyItem: LobbyItem) {
        scope?.launch {
            val singleCard = lobbyItem as SingleCard
            getLobbyImpressionEventData(singleCard)?.let {
                eventManager.sendEvent(LobbyImpressionEvent(it))
            }
        }
    }

    private var frameListener: EglRenderer.FrameListener? = null

    protected fun startLiveVideo(
        renderer: SurfaceViewRenderer,
        controller: NewReyesController,
        onConnectCallback: () -> Unit
    ) {
        scope?.launch {
            launch {
                controller.connectionChannel.consumeEach { feedInfo ->
                    val audioTrack = feedInfo.connectionInfo.audioTrack
                    audioTrack?.setEnabled(false)
                    // audioTrack?.setVolume(0.0)
                    if (feedInfo.role == NewReyes.Feed.Role.primary) {
                        val videoTrack = feedInfo.connectionInfo.videoTrack
                        configureRenderer(renderer, feedInfo.feed, videoTrack)
                        frameListener = EglRenderer.FrameListener {
                            launch(Dispatchers.Main) {
                                renderer.removeFrameListener(frameListener)
                                onConnectCallback()
                            }
                        }
                        renderer.addFrameListener(frameListener, 1.0f)
                    }
                }
            }
            launch {
                controller.stateChangeChannel.consumeEach { list ->
                    list.forEach { stateChange ->
                        when (stateChange) {
                            is NewReyesController.StateChange.FeedRemoved -> {
                                // TODO remove sink for primary video
                                // TODO videoTrack.removeSink(binding.primaryViewRenderer)
                            }
                        }
                    }
                }
            }
            launch { controller.feedChannel.consumeEach { } }
            launch { controller.errorChannel.consumeEach { } }
            launch { controller.feedQualityChannel.consumeEach { } }
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
}

open class LiveBroadcastCard @AssistedInject constructor(
    @Assisted val binding: LiveBroadcastCardBinding,
    @Assisted tags: Map<String, Lobby.Tag>,
    @Assisted content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    @ThemeFollowedLobby private val followedTheme: UserTheme,
    @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme,
    @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
    @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    @Assisted payloadId: String?,
    @Assisted private val scope: CoroutineScope? = null,
    private val stageControllerFactory: NewReyesController.Factory,
    private val surfaceViewRendererTuner: SurfaceViewRendererTuner,
    private val autoPlayConfig: AutoPlayConfig,
    clock: Clock,
    eventManager: EventManager
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, payloadId, scope, clock, eventManager) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            binding: LiveBroadcastCardBinding,
            tags: Map<String, Lobby.Tag>,
            content: Map<String, Lobby.Content>,
            payloadId: String?,
            scope: CoroutineScope? = null
        ): LiveBroadcastCard
    }

    override fun recycle() {
        super.recycle()
        binding.primaryViewRenderer.release()
    }

    override fun configure(item: LobbyItem) {
        super.configure(item)
        stageController?.close()
        binding.previewImageView.isInvisible = false
        binding.primaryViewRenderer.release()
        val liveBroadcastItem = item as LiveBroadcast
        val broadcast = liveBroadcastItem.broadcaster.broadcast ?: error("Unexpected broadcast state")
        val game = content[broadcast.contentId]
        if (game != null) {
            picasso.load(game.iconImageUrl).into(binding.gameLogoImageView)
        } else {
            binding.gameLogoImageView.setImageDrawable(null)
        }
        liveBroadcastItem.broadcaster.user.let {
            binding.moreButton.setOnClickListener(MoreButtonClickListener(it.caid, it.username))
        }
        val cardClickListener: (View) -> Unit = {
            getLobbyClickedEventData(item)?.let { eventData ->
                scope?.launch { eventManager.sendEvent(LobbyCardClickedEvent(eventData)) }
            }
            val action =
                LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment(item.broadcaster.user.username)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
        previewImageView.setOnClickListener(cardClickListener)
        binding.primaryViewRenderer.setOnClickListener(cardClickListener)
        if (autoPlayConfig.isAutoPlayEnabled(liveBroadcastItem.broadcaster.displayOrder)) {
            surfaceViewRendererTuner.configure(binding.primaryViewRenderer)
            val renderer = binding.primaryViewRenderer
            val username = liveBroadcastItem.broadcaster.user.username
            val controller = stageControllerFactory.create(username, true)
            stageController = controller
            startLiveVideo(renderer, controller) {
                binding.previewImageView.isInvisible = true
            }
        }
    }
}

class LiveBroadcastWithFriendsCard @AssistedInject constructor(
    @Assisted val binding: LiveBroadcastWithFriendsCardBinding,
    @Assisted tags: Map<String, Lobby.Tag>,
    @Assisted content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    @ThemeFollowedLobby private val followedTheme: UserTheme,
    @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme,
    @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
    @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    @Assisted payloadId: String?,
    @Assisted private val scope: CoroutineScope?,
    private val stageControllerFactory: NewReyesController.Factory,
    private val surfaceViewRendererTuner: SurfaceViewRendererTuner,
    private val autoPlayConfig: AutoPlayConfig,
    clock: Clock,
    eventManager: EventManager
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, payloadId, scope, clock, eventManager) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            binding: LiveBroadcastWithFriendsCardBinding,
            tags: Map<String, Lobby.Tag>,
            content: Map<String, Lobby.Content>,
            payloadId: String?,
            scope: CoroutineScope?
        ): LiveBroadcastWithFriendsCard
    }

    override fun recycle() {
        super.recycle()
        binding.primaryViewRenderer.release()
    }

    override fun configure(item: LobbyItem) {
        super.configure(item)
        stageController?.close()
        binding.previewImageView.isInvisible = false
        binding.primaryViewRenderer.release()
        val liveBroadcastItem = item as LiveBroadcastWithFriends
        binding.previewImageView.clipToOutline = true
        val broadcaster = item.broadcaster
        val context = itemView.context
        val friendsWatchingString = formatFriendsWatchingString(context, broadcaster)
        friendsWatchingString?.let { binding.friendsWatchingTextView.formatUsernameAsHtml(picasso, it, true, R.dimen.avatar_friends_watching) }
        val broadcast = liveBroadcastItem.broadcaster.broadcast ?: error("Unexpected broadcast state")
        val game = content[broadcast.contentId]
        if (game != null) {
            picasso.load(game.iconImageUrl).into(binding.gameLogoImageView)
        } else {
            binding.gameLogoImageView.setImageDrawable(null)
        }
        liveBroadcastItem.broadcaster.user.let {
            binding.moreButton.setOnClickListener(MoreButtonClickListener(it.caid, it.username))
        }
        val cardClickListener: (View) -> Unit = {
            getLobbyClickedEventData(item)?.let { eventData ->
                scope?.launch { eventManager.sendEvent(LobbyCardClickedEvent(eventData)) }
            }
            val action =
                LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment(broadcaster.user.username)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
        previewImageView.setOnClickListener(cardClickListener)
        binding.primaryViewRenderer.setOnClickListener(cardClickListener)
        if (autoPlayConfig.isAutoPlayEnabled(broadcaster.displayOrder)) {
            surfaceViewRendererTuner.configure(binding.primaryViewRenderer)
            val renderer = binding.primaryViewRenderer
            val username = liveBroadcastItem.broadcaster.user.username
            val controller = stageControllerFactory.create(username, true)
            stageController = controller
            startLiveVideo(renderer, controller) {
                binding.previewImageView.isInvisible = true
            }
        }
    }
}

private class MoreButtonClickListener(val caid: CAID, val username: String) : View.OnClickListener {
    override fun onClick(v: View?) {
        v?.findNavController()?.navigateToReportOrIgnoreDialog(caid, username, false)
    }
}

class PreviousBroadcastCard @AssistedInject constructor(
    @Assisted val binding: PreviousBroadcastCardBinding,
    @Assisted tags: Map<String, Lobby.Tag>,
    @Assisted content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    @ThemeFollowedLobby private val followedTheme: UserTheme,
    @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme,
    @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
    @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    @Assisted payloadId: String?,
    @Assisted scope: CoroutineScope,
    clock: Clock,
    eventManager: EventManager
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, payloadId, scope, clock, eventManager) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            binding: PreviousBroadcastCardBinding,
            tags: Map<String, Lobby.Tag>,
            content: Map<String, Lobby.Content>,
            payloadId: String?,
            scope: CoroutineScope
        ): PreviousBroadcastCard
    }

    override val cornerType: RoundedCornersTransformation.CornerType = RoundedCornersTransformation.CornerType.ALL
    override val isLight: Boolean = true

    override fun configure(item: LobbyItem) {
        super.configure(item)
        val previousBroadcastItem = item as PreviousBroadcast
        val broadcast = previousBroadcastItem.broadcaster.lastBroadcast ?: error("Unexpected broadcast state")
        binding.viewModel = item
        binding.nameTextView.text = previousBroadcastItem.broadcaster.user.name
        binding.lastBroadcastTextView.text = broadcast.dateText
        itemView.setOnClickListener { viewProfile(item.broadcaster.user.caid) }
        binding.followButton.setTextColor(ContextCompat.getColor(binding.followButton.context, R.color.white))
    }
}

class ListCard @AssistedInject constructor(
    @Assisted val binding: CardListBinding,
    @Assisted private val tags: Map<String, Lobby.Tag>,
    @Assisted private val content: Map<String, Lobby.Content>,
    @Assisted private val payloadId: String?,
    recycledViewPool: RecyclerView.RecycledViewPool,
    private val lobbyAdapter: LobbyAdapter
) : LobbyViewHolder(binding.root) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            binding: CardListBinding,
            tags: Map<String, Lobby.Tag>,
            content: Map<String, Lobby.Content>,
            payloadId: String?
        ): ListCard
    }

    private val snapHelper = LinearSnapHelper()
    private val edgeOffset = binding.root.resources.getDimension(R.dimen.lobby_card_side_margin).toInt()
    private val insetOffset = binding.root.resources.getDimension(R.dimen.lobby_card_narrow_margin).toInt()

    init {
        lobbyAdapter.isMiniStyle = true
        binding.cardListRecyclerView.adapter = lobbyAdapter
        binding.cardListRecyclerView.run {
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
                    when (itemPosition) {
                        0 -> outRect.set(edgeOffset, 0, insetOffset, 0)
                        (adapter?.itemCount ?: 0) - 1 -> outRect.set(insetOffset, 0, edgeOffset, 0)
                        else -> outRect.set(insetOffset, 0, insetOffset, 0)
                    }
                }
            })
            setRecycledViewPool(recycledViewPool)
        }
        snapHelper.attachToRecyclerView(binding.cardListRecyclerView)
        itemView.updateLayoutParams<RecyclerView.LayoutParams> {
            marginStart = 0
            marginEnd = 0
        }
    }
    override fun configure(item: LobbyItem) {
        val cardList = item as CardList
        if (payloadId != null) {
            lobbyAdapter.submitList(cardList.cards, tags, content, payloadId)
        }
    }
}

class LiveBroadcastPickerCard @AssistedInject constructor(
    @Assisted binding: LiveBroadcastCardBinding,
    @Assisted val callback: Callback?,
    @Assisted tags: Map<String, Lobby.Tag>,
    @Assisted content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    @ThemeFollowedLobby private val followedTheme: UserTheme,
    @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme,
    @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
    @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    @Assisted scope: CoroutineScope,
    stageControllerFactory: NewReyesController.Factory,
    surfaceViewRendererTuner: SurfaceViewRendererTuner,
    autoPlayConfig: AutoPlayConfig,
    clock: Clock,
    eventManager: EventManager
) : LiveBroadcastCard(binding, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, null, scope, stageControllerFactory, surfaceViewRendererTuner, autoPlayConfig, clock, eventManager) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            binding: LiveBroadcastCardBinding,
            tags: Map<String, Lobby.Tag>,
            content: Map<String, Lobby.Content>,
            scope: CoroutineScope,
            callback: Callback?
        ): LiveBroadcastPickerCard
    }

    // The picker card for live hosting is not on the lobby, so the payload id is null and we don't track it.
    override fun configure(item: LobbyItem) {
        super.configure(item)
        val liveBroadcastItem = item as LiveBroadcast
        previewImageView.setOnClickListener {
            liveBroadcastItem.broadcaster.broadcast?.id?.let {
                callback?.onPreviewImageClicked(it)
            }
        }
        liveBroadcastItem.broadcaster.user.let { user ->
            binding.moreButton.setOnClickListener {
                callback?.onMoreButtonClicked(user.caid, user.username)
            }
        }
    }

    override fun viewProfile(caid: CAID) {
        // prevent navigating from a picker card to the user profile because that will end the broadcast.
    }

    interface Callback {
        fun onPreviewImageClicked(broadcastId: String)
        fun onMoreButtonClicked(caid: CAID, username: String)
    }
}

class UpcomingButtonCard(
    val binding: UpcomingButtonCardBinding,
    val callback: Callback?
) : LobbyViewHolder(binding.root) {

    override fun configure(item: LobbyItem) {
        binding.viewUpcomingButton.setOnClickListener {
            callback?.onButtonClicked()
        }
    }

    interface Callback {
        fun onButtonClicked()
    }
}

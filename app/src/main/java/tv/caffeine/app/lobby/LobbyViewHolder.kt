package tv.caffeine.app.lobby

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.LobbyCardClickedEvent
import tv.caffeine.app.api.LobbyClickedEventData
import tv.caffeine.app.api.LobbyFollowClickedEvent
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.CardListBinding
import tv.caffeine.app.databinding.LiveBroadcastCardBinding
import tv.caffeine.app.databinding.LiveBroadcastWithFriendsCardBinding
import tv.caffeine.app.databinding.LobbyAvatarCardBinding
import tv.caffeine.app.databinding.LobbyFollowPeopleCardBinding
import tv.caffeine.app.databinding.LobbyHeaderBinding
import tv.caffeine.app.databinding.LobbySubtitleBinding
import tv.caffeine.app.databinding.PreviousBroadcastCardBinding
import tv.caffeine.app.databinding.UpcomingButtonCardBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate
import java.util.concurrent.TimeUnit

sealed class LobbyViewHolder(
    itemView: View,
    val tags: Map<String, Lobby.Tag>,
    val content: Map<String, Lobby.Content>,
    val followManager: FollowManager,
    val followedTheme: UserTheme,
    val notFollowedTheme: UserTheme,
    val followedThemeLight: UserTheme,
    val notFollowedThemeLight: UserTheme,
    val picasso: Picasso
) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: LobbyItem) {
        itemView.tag = item.itemType
        configure(item)
    }
    protected abstract fun configure(item: LobbyItem)
}

class AvatarCard(
    val binding: LobbyAvatarCardBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
    override fun configure(item: LobbyItem) {
        binding.username = (item as WelcomeCard).username
        itemView.setOnClickListener {
            val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToMyProfileFragment(true)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}

class FollowPeopleCard(
    val binding: LobbyFollowPeopleCardBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
    override fun configure(item: LobbyItem) {
        binding.displayMessage = (item as FollowPeople).displayMessage
    }
}

class HeaderCard(
    val binding: LobbyHeaderBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
    override fun configure(item: LobbyItem) {
        binding.viewModel = item as Header
    }
}

class SubtitleCard(
    val binding: LobbySubtitleBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
    override fun configure(item: LobbyItem) {
        binding.viewModel = item as Subtitle
    }
}

abstract class BroadcasterCard(
    view: View,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    private val lobbyId: String? = null,
    private val scope: CoroutineScope? = null,
    private val clock: Clock? = null,
    protected val eventsService: EventsService? = null
) : LobbyViewHolder(view, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
    protected val previewImageView: ImageView = view.findViewById(R.id.preview_image_view)
    private val avatarImageView: ImageView = view.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = view.findViewById(R.id.username_text_view)
    private val broadcastTitleTextView: TextView = view.findViewById(R.id.broadcast_title_text_view)
    private val tagTextView: TextView = view.findViewById(R.id.tag_text_view)
    private val followButton: Button = view.findViewById(R.id.follow_button)
    private val dotTextView: TextView? = view.findViewById(R.id.dot_text_view)
    private val pipImageView: ImageView? = itemView.findViewById(R.id.pip_image_view)

    protected open val cornerType: RoundedCornersTransformation.CornerType = RoundedCornersTransformation.CornerType.TOP
    protected open val isLight: Boolean = false
    private val roundedCornersTransformation by lazy { RoundedCornersTransformation(itemView.resources.getDimension(R.dimen.lobby_card_rounding_radius).toInt(), 0, cornerType) }

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
                getLobbyClickedEventData(singleCard)?.let { eventsService?.sendEvent(LobbyFollowClickedEvent(it)) }
                scope?.launch {
                    followManager.followUser(caid, object : FollowManager.FollowCompletedCallback {
                        override fun onUserFollowed() {
                            configureUser(singleCard.broadcaster.user, null)
                        }
                    })
                }
            }

            override fun unfollow(caid: CAID) {
            }
        })
        configureUser(singleCard.broadcaster.user, followHandler)
        dotTextView?.isVisible = !followManager.isFollowing(singleCard.broadcaster.user.caid)
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

    @VisibleForTesting fun getLobbyClickedEventData(singleCard: SingleCard): LobbyClickedEventData? {
        if (clock == null || lobbyId == null) return null
        val broadcast = singleCard.broadcaster.broadcast ?: return null
        val timestamp = TimeUnit.MILLISECONDS.toSeconds(clock.millis())
        return LobbyClickedEventData(lobbyId, singleCard.broadcaster.user.caid, broadcast.id, timestamp.toString())
    }
}

open class LiveBroadcastCard(
    val binding: LiveBroadcastCardBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    lobbyId: String? = null,
    scope: CoroutineScope? = null,
    clock: Clock? = null,
    eventsService: EventsService? = null
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, lobbyId, scope, clock, eventsService) {

    override fun configure(item: LobbyItem) {
        super.configure(item)
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
        previewImageView.setOnClickListener {
            getLobbyClickedEventData(item)?.let { eventsService?.sendEvent(LobbyCardClickedEvent(it)) }
            val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment(item.broadcaster.user.username)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}

class LiveBroadcastWithFriendsCard(
    val binding: LiveBroadcastWithFriendsCardBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    lobbyId: String?,
    scope: CoroutineScope?,
    clock: Clock?,
    eventsService: EventsService?
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, lobbyId, scope, clock, eventsService) {
    override fun configure(item: LobbyItem) {
        super.configure(item)
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
        previewImageView.setOnClickListener {
            getLobbyClickedEventData(item)?.let { eventsService?.sendEvent(LobbyCardClickedEvent(it)) }
            val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment(broadcaster.user.username)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}

private class MoreButtonClickListener(val caid: CAID, val username: String) : View.OnClickListener {
    override fun onClick(v: View?) {
        v?.findNavController()?.navigateToReportOrIgnoreDialog(caid, username, false)
    }
}

class PreviousBroadcastCard(
    val binding: PreviousBroadcastCardBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
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

class ListCard(
    val binding: CardListBinding,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso,
    recycledViewPool: RecyclerView.RecycledViewPool,
    dispatchConfig: DispatchConfig,
    clock: Clock,
    eventsService: EventsService
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
    private val snapHelper = LinearSnapHelper()
    private val edgeOffset = binding.root.resources.getDimension(R.dimen.lobby_card_side_margin).toInt()
    private val insetOffset = binding.root.resources.getDimension(R.dimen.lobby_card_narrow_margin).toInt()
    private val lobbyAdapter = LobbyAdapter(dispatchConfig, followManager, recycledViewPool, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, clock, eventsService)
    init {
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
        lobbyAdapter.submitList(cardList.cards, tags, content)
    }
}

class LiveBroadcastPickerCard(
    binding: LiveBroadcastCardBinding,
    val callback: Callback?,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso
) : LiveBroadcastCard(binding, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {

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
    val callback: Callback?,
    tags: Map<String, Lobby.Tag>,
    content: Map<String, Lobby.Content>,
    followManager: FollowManager,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    followedThemeLight: UserTheme,
    notFollowedThemeLight: UserTheme,
    picasso: Picasso
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso) {
    override fun configure(item: LobbyItem) {
        binding.viewUpcomingButton.setOnClickListener {
            callback?.onButtonClicked()
        }
    }

    interface Callback {
        fun onButtonClicked()
    }
}

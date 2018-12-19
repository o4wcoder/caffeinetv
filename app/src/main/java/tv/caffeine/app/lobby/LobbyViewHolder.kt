package tv.caffeine.app.lobby

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.*
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate

sealed class LobbyViewHolder(
        itemView: View,
        val tags: Map<String, Lobby.Tag>,
        val content: Map<String, Lobby.Content>,
        val followManager: FollowManager,
        val followedTheme: UserTheme,
        val notFollowedTheme: UserTheme,
        val followedThemeLight: UserTheme,
        val notFollowedThemeLight: UserTheme
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
        notFollowedThemeLight: UserTheme
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
    override fun configure(item: LobbyItem) {
        itemView.setOnClickListener {
            val action = LobbyFragmentDirections.actionLobbyFragmentToMyProfileFragment()
            action.setLaunchAvatarSelection(true)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
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
        notFollowedThemeLight: UserTheme
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
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
        notFollowedThemeLight: UserTheme
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
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
        notFollowedThemeLight: UserTheme
) : LobbyViewHolder(view, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
    protected val previewImageView: ImageView = view.findViewById(R.id.preview_image_view)
    private val avatarImageView: ImageView = view.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = view.findViewById(R.id.username_text_view)
    private val broadcastTitleTextView: TextView = view.findViewById(R.id.broadcast_title_text_view)
    private val tagTextView: TextView = view.findViewById(R.id.tag_text_view)
    private val followButton: Button = view.findViewById(R.id.follow_button)
    private val dotTextView: TextView? = view.findViewById(R.id.dot_text_view)

    protected open val cornerType: RoundedCornersTransformation.CornerType = RoundedCornersTransformation.CornerType.TOP
    protected open val isLight: Boolean = false
    private val roundedCornersTransformation by lazy { RoundedCornersTransformation(itemView.resources.getDimension(R.dimen.lobby_card_rounding_radius).toInt(), 0, cornerType) }

    override fun configure(item: LobbyItem) {
        val singleCard = item as SingleCard
        val broadcast = singleCard.broadcaster.broadcast ?: singleCard.broadcaster.lastBroadcast ?: error("Unexpected lobby item state")
        Picasso.get()
                .load(broadcast.previewImageUrl)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.default_lobby_image)
                .transform(roundedCornersTransformation)
                .into(previewImageView)
        val isLive = singleCard.broadcaster.broadcast != null
        singleCard.broadcaster.user.configure(
                avatarImageView,
                usernameTextView,
                followButton,
                followManager,
                false,
                null,
                R.dimen.avatar_size,
                if (isLight) followedThemeLight else followedTheme,
                if (isLight) notFollowedThemeLight else notFollowedTheme
        )
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
    }

    fun viewProfile(caid: String) {
        val action = LobbyDirections.actionGlobalProfileFragment(caid)
        Navigation.findNavController(itemView).safeNavigate(action)
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
        notFollowedThemeLight: UserTheme
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
    override fun configure(item: LobbyItem) {
        super.configure(item)
        val liveBroadcastItem = item as LiveBroadcast
        val broadcast = liveBroadcastItem.broadcaster.broadcast ?: error("Unexpected broadcast state")
        val game = content[broadcast.contentId]
        if (game != null) {
            Picasso.get().load(game.iconImageUrl).into(binding.gameLogoImageView)
        } else {
            binding.gameLogoImageView.setImageDrawable(null)
        }
        liveBroadcastItem.broadcaster.user.let {
            binding.moreButton.setOnClickListener(MoreButtonClickListener(it.caid, it.username))
        }
        previewImageView.setOnClickListener {
            val action = LobbyFragmentDirections.actionLobbyFragmentToStageFragment(item.broadcaster.user.caid)
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
        notFollowedThemeLight: UserTheme
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
    override fun configure(item: LobbyItem) {
        super.configure(item)
        val liveBroadcastItem = item as LiveBroadcastWithFriends
        binding.previewImageView.clipToOutline = true
        val firstFriendVerified = item.broadcaster.followingViewers.firstOrNull()?.isVerified == true
        val singleFriendWatchingResId = if (firstFriendVerified) R.string.verified_user_watching else R.string.user_watching
        val multipleFriendsWatchingResId = if (firstFriendVerified) R.plurals.verified_user_and_friends_watching else R.plurals.user_and_friends_watching
        val friendsWatchingString = when(item.broadcaster.followingViewersCount) {
            0 -> null
            1 -> itemView.context.getString(singleFriendWatchingResId, item.broadcaster.followingViewers[0].username, item.broadcaster.followingViewers[0].avatarImageUrl)
            else -> itemView.context.resources.getQuantityString(multipleFriendsWatchingResId, item.broadcaster.followingViewersCount - 1, item.broadcaster.followingViewers[0].username, item.broadcaster.followingViewers[0].avatarImageUrl, item.broadcaster.followingViewersCount - 1)
        }
        friendsWatchingString?.let { binding.friendsWatchingTextView.formatUsernameAsHtml(it, true, R.dimen.avatar_lobby_friend_watching) }
        val broadcast = liveBroadcastItem.broadcaster.broadcast ?: error("Unexpected broadcast state")
        val game = content[broadcast.contentId]
        if (game != null) {
            Picasso.get().load(game.iconImageUrl).into(binding.gameLogoImageView)
        } else {
            binding.gameLogoImageView.setImageDrawable(null)
        }
        liveBroadcastItem.broadcaster.user.let {
            binding.moreButton.setOnClickListener(MoreButtonClickListener(it.caid, it.username))
        }
        previewImageView.setOnClickListener {
            val action = LobbyFragmentDirections.actionLobbyFragmentToStageFragment(item.broadcaster.user.caid)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}

private class MoreButtonClickListener(val caid: String, val username: String) : View.OnClickListener {
    override fun onClick(v: View?) {
        (v?.context as? FragmentActivity)?.supportFragmentManager?.navigateToReportOrIgnoreDialog(caid, username, false)
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
        notFollowedThemeLight: UserTheme
) : BroadcasterCard(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
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
        recycledViewPool: RecyclerView.RecycledViewPool
) : LobbyViewHolder(binding.root, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight) {
    private val snapHelper = LinearSnapHelper()
    private val edgeOffset = binding.root.resources.getDimension(R.dimen.lobby_card_side_margin).toInt()
    private val insetOffset = binding.root.resources.getDimension(R.dimen.lobby_card_narrow_margin).toInt()
    private val lobbyAdapter = LobbyAdapter(followManager, recycledViewPool, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight)
    init {
        binding.cardListRecyclerView.adapter = lobbyAdapter
        binding.cardListRecyclerView.run {
            addItemDecoration(object: RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
                    when(itemPosition) {
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


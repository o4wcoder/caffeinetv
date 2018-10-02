package tv.caffeine.app.lobby

import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.*
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure

sealed class LobbyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun configure(item: LobbyItem, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme)
}

class HeaderCard(val binding: LobbyHeaderBinding) : LobbyViewHolder(binding.root) {
    override fun configure(item: LobbyItem, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        binding.viewModel = item as Header
    }
}

class SubtitleCard(val binding: LobbySubtitleBinding) : LobbyViewHolder(binding.root) {
    override fun configure(item: LobbyItem, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        binding.viewModel = item as Subtitle
    }
}

abstract class BroadcasterCard(view: View) : LobbyViewHolder(view) {
    private val previewImageView: ImageView = view.findViewById(R.id.preview_image_view)
    private val avatarImageView: ImageView = view.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = view.findViewById(R.id.username_text_view)
    private val broadcastTitleTextView: TextView = view.findViewById(R.id.broadcast_title_text_view)
    private val tagTextView: TextView = view.findViewById(R.id.tag_text_view)
    private val followButton: Button = view.findViewById(R.id.follow_button)

    private val roundedCornersTransformation = RoundedCornersTransformation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, itemView.resources.displayMetrics).toInt(), 0)

    override fun configure(item: LobbyItem, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
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
        singleCard.broadcaster.user.configure(avatarImageView, usernameTextView, followButton, followManager, !isLive, R.dimen.avatar_size, followedTheme, notFollowedTheme)
        avatarImageView.setOnClickListener { viewProfile(item.broadcaster.user.caid) }
        usernameTextView.setOnClickListener { viewProfile(item.broadcaster.user.caid) }
        broadcastTitleTextView.text = broadcast.name
        val tag = tags[singleCard.broadcaster.tagId]
        tagTextView.isVisible = tag != null
        if (tag != null) {
            tagTextView.text = tag.name
            tagTextView.setTextColor(tag.color.toColorInt())
        }
    }

    fun viewProfile(caid: String) {
        val action = LobbyFragmentDirections.actionLobbyFragmentToProfileFragment(caid)
        Navigation.findNavController(itemView).navigate(action)
    }
}

class LiveBroadcastCard(val binding: LiveBroadcastCardBinding) : BroadcasterCard(binding.root) {
    override fun configure(item: LobbyItem, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        super.configure(item, tags, content, followManager, followedTheme, notFollowedTheme)
        val liveBroadcastItem = item as LiveBroadcast
        binding.friendsWatchingTextView.isVisible = item.broadcaster.followingViewersCount > 0
        binding.friendsWatchingKiltView.isVisible = item.broadcaster.followingViewersCount > 0
        binding.friendsWatchingTextView.text = when(item.broadcaster.followingViewersCount) {
            0 -> null
            1 -> itemView.context.getString(R.string.user_watching, item.broadcaster.followingViewers[0].username)
            else -> itemView.context.resources.getQuantityString(R.plurals.user_and_friends_watching, item.broadcaster.followingViewersCount - 1, item.broadcaster.followingViewers[0].username, item.broadcaster.followingViewersCount - 1)
        }
        val broadcast = liveBroadcastItem.broadcaster.broadcast ?: error("Unexpected broadcast state")
        val game = content[broadcast.contentId]
        if (game != null) {
            Picasso.get().load(game.iconImageUrl).into(binding.gameLogoImageView)
        } else {
            binding.gameLogoImageView.setImageDrawable(null)
        }
        itemView.setOnClickListener {
            val action = LobbyFragmentDirections.actionLobbyFragmentToStageFragment(item.broadcaster.user.username)
            Navigation.findNavController(itemView).navigate(action)
        }
    }
}

class PreviousBroadcastCard(val binding: PreviousBroadcastCardBinding) : BroadcasterCard(binding.root) {
    override fun configure(item: LobbyItem, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        super.configure(item, tags, content, followManager, followedTheme, notFollowedTheme)
        val previousBroadcastItem = item as PreviousBroadcast
        val broadcast = previousBroadcastItem.broadcaster.lastBroadcast ?: error("Unexpected broadcast state")
        binding.viewModel = item
        binding.nameTextView.text = previousBroadcastItem.broadcaster.user.name
        binding.lastBroadcastTextView.text = broadcast.dateText
        itemView.setOnClickListener { viewProfile(item.broadcaster.user.caid) }
    }
}

class ListCard(val binding: CardListBinding, private val recycledViewPool: RecyclerView.RecycledViewPool) : LobbyViewHolder(binding.root) {
    private val snapHelper = LinearSnapHelper()
    init {
        binding.cardListRecyclerView.setRecycledViewPool(recycledViewPool)
        snapHelper.attachToRecyclerView(binding.cardListRecyclerView)
    }
    override fun configure(item: LobbyItem, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        val cardList = item as CardList
        val lobbyAdapter = LobbyAdapter(followManager, recycledViewPool, followedTheme, notFollowedTheme)
        binding.cardListRecyclerView.adapter = lobbyAdapter
        lobbyAdapter.submitList(cardList.cards, tags, content)
    }
}


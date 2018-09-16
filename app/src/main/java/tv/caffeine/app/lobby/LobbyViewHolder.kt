package tv.caffeine.app.lobby

import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import tv.caffeine.app.R
import tv.caffeine.app.api.Api
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure

sealed class LobbyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme)
}

class HeaderCard(view: View) : LobbyViewHolder(view) {
    private val headerTextView: TextView = view.findViewById(R.id.header_text_view)
    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        val header = item as LobbyItem.Header
        headerTextView.text = header.text
    }
}

class SubtitleCard(view: View) : LobbyViewHolder(view) {
    private val subtitleTextView: TextView = view.findViewById(R.id.subtitle_text_view)
    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        val subtitle = item as LobbyItem.Subtitle
        subtitleTextView.text = subtitle.text
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

    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        val singleCard = item as LobbyItem.SingleCard
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

class LiveBroadcastCard(view: View) : BroadcasterCard(view) {
    private val friendsWatchingTextView: TextView = view.findViewById(R.id.friends_watching_text_view)
    private val friendsWatchingKiltView: View = view.findViewById(R.id.friends_watching_kilt_view)
    private val gameLogoImageView: ImageView = view.findViewById(R.id.game_logo_image_view)

    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        super.configure(item, tags, content, followManager, followedTheme, notFollowedTheme)
        val liveBroadcastItem = item as LobbyItem.LiveBroadcast
        friendsWatchingTextView.isVisible = item.broadcaster.followingViewersCount > 0
        friendsWatchingKiltView.isVisible = item.broadcaster.followingViewersCount > 0
        when(item.broadcaster.followingViewersCount) {
            0 -> friendsWatchingTextView.text = null
            1 -> friendsWatchingTextView.text =  itemView.context.getString(R.string.user_watching, item.broadcaster.followingViewers[0].username)
            else -> friendsWatchingTextView.text = itemView.context.resources.getQuantityString(R.plurals.user_and_friends_watching, item.broadcaster.followingViewersCount - 1, item.broadcaster.followingViewers[0].username, item.broadcaster.followingViewersCount - 1)
        }
        val broadcast = liveBroadcastItem.broadcaster.broadcast ?: error("Unexpected broadcast state")
        val game = content[broadcast.contentId]
        if (game != null) {
            Picasso.get().load(game.iconImageUrl).into(gameLogoImageView)
        } else {
            gameLogoImageView.setImageDrawable(null)
        }
        itemView.setOnClickListener {
            val action = LobbyFragmentDirections.actionLobbyFragmentToStageFragment(item.broadcaster.user.username)
            Navigation.findNavController(itemView).navigate(action)
        }
    }
}

class PreviousBroadcastCard(view: View) : BroadcasterCard(view) {
    private val nameTextView: TextView = view.findViewById(R.id.name_text_view)
    private val lastBroadcastTextView: TextView = view.findViewById(R.id.last_broadcast_text_view)

    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        super.configure(item, tags, content, followManager, followedTheme, notFollowedTheme)
        val previousBroadcastItem = item as LobbyItem.PreviousBroadcast
        val broadcast = previousBroadcastItem.broadcaster.lastBroadcast ?: error("Unexpected broadcast state")
        nameTextView.text = previousBroadcastItem.broadcaster.user.name
        lastBroadcastTextView.text = broadcast.dateText
        itemView.setOnClickListener { viewProfile(item.broadcaster.user.caid) }
    }
}

class ListCard(view: View, private val recycledViewPool: RecyclerView.RecycledViewPool) : LobbyViewHolder(view) {
    private val recyclerView: RecyclerView = view.findViewById(R.id.card_list_recycler_view)
    private val snapHelper = LinearSnapHelper()
    init {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, RecyclerView.HORIZONTAL, false)
        recyclerView.setRecycledViewPool(recycledViewPool)
        snapHelper.attachToRecyclerView(recyclerView)
    }
    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        val cardList = item as LobbyItem.CardList
        recyclerView.adapter = LobbyAdapter(cardList.cards, tags, content, followManager, recycledViewPool, followedTheme, notFollowedTheme)
    }
}


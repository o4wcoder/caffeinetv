package tv.caffeine.app.lobby

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.Api

sealed class LobbyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>)
}

class HeaderCard(view: View) : LobbyViewHolder(view) {
    private val headerTextView: TextView = view.findViewById(R.id.header_text_view)
    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
        val item = item as LobbyItem.Header
        headerTextView.text = item.text
    }
}

class SubtitleCard(view: View) : LobbyViewHolder(view) {
    private val subtitleTextView: TextView = view.findViewById(R.id.subtitle_text_view)
    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
        val item = item as LobbyItem.Subtitle
        subtitleTextView.text = item.text
    }
}

abstract class BroadcasterCard(view: View) : LobbyViewHolder(view) {
    private val previewImageView: ImageView = view.findViewById(R.id.preview_image_view)
    private val avatarImageView: ImageView = view.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = view.findViewById(R.id.username_text_view)
    private val broadcastTitleTextView: TextView = view.findViewById(R.id.broadcast_title_text_view)
    private val tagTextView: TextView = view.findViewById(R.id.tag_text_view)

    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
        val item = item as LobbyItem.SingleCard
        val broadcast = item.broadcaster.broadcast ?: item.broadcaster.lastBroadcast ?: error("Unexpected lobby item state")
        val previewImageUrl = "https://images.caffeine.tv${broadcast.previewImagePath}"
        Picasso.get()
                .load(previewImageUrl)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.default_lobby_image)
                .transform(RoundedCornersTransformation(40, 0)) // TODO: multiply by display density
                .into(previewImageView)
        Timber.d("Preview image: $previewImageUrl")
        val avatarImageUrl = "https://images.caffeine.tv${item.broadcaster.user.avatarImagePath}"
        Picasso.get()
                .load(avatarImageUrl)
                .placeholder(R.drawable.default_avatar)
                .transform(CropCircleTransformation())
                .into(avatarImageView)
        Timber.d("Avatar image: $avatarImageUrl")
        usernameTextView.text = item.broadcaster.user.username
        if (item.broadcaster.user.isVerified) {
            usernameTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.verified_large, 0)
        } else {
            usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
        broadcastTitleTextView.text = broadcast.name
        val tag = tags[item.broadcaster.tagId]
        tagTextView.isVisible = tag != null
        if (tag != null) {
            tagTextView.text = tag.name
            tagTextView.setTextColor(tag.color.toColorInt())
        }
    }
}

class LiveBroadcastCard(view: View) : BroadcasterCard(view) {
    private val friendsWatchingTextView: TextView = view.findViewById(R.id.friends_watching_text_view)
    private val gameLogoImageView: ImageView = view.findViewById(R.id.game_logo_image_view)

    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
        super.configure(item, tags, content)
        val liveBroadcastItem = item as LobbyItem.LiveBroadcast
        friendsWatchingTextView.isVisible = item.broadcaster.followingViewersCount > 0
        when(item.broadcaster.followingViewersCount) {
            0 -> friendsWatchingTextView.text = null
            1 -> friendsWatchingTextView.text =  itemView.context.getString(R.string.user_watching, item.broadcaster.followingViewers[0].username)
            else -> friendsWatchingTextView.text = itemView.context.resources.getQuantityString(R.plurals.user_and_friends_watching, item.broadcaster.followingViewersCount - 1, item.broadcaster.followingViewers[0].username, item.broadcaster.followingViewersCount - 1)
        }
        val broadcast = liveBroadcastItem.broadcaster.broadcast ?: error("Unexpected broadcast state")
        val game = content[broadcast.contentId]
        if (game != null) {
            val gameLogoImageUrl = "https://images.caffeine.tv${game.iconImagePath}"
            Picasso.get().load(gameLogoImageUrl).into(gameLogoImageView)
        } else {
            gameLogoImageView.setImageDrawable(null)
        }
        itemView.setOnClickListener {
            // TODO: switch to safeargs when their code gen is fixed
//            val action = LobbyFragmentDirections.actionLobbyFragmentToStage(item.broadcaster.user.stageId, item.broadcaster.user.username)
//            Navigation.findNavController(itemView).navigate(action)
            val args = Bundle().apply {
                putString("stageIdentifier", item.broadcaster.user.stageId)
                putString("broadcaster", item.broadcaster.user.username)
            }
            Navigation.findNavController(itemView).navigate(R.id.stage, args)
        }
    }
}

class PreviousBroadcastCard(view: View) : BroadcasterCard(view) {
    private val nameTextView: TextView = view.findViewById(R.id.name_text_view)
    private val lastBroadcastTextView: TextView = view.findViewById(R.id.last_broadcast_text_view)

    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
        super.configure(item, tags, content)
        val previousBroadcastItem = item as LobbyItem.PreviousBroadcast
        val broadcast = previousBroadcastItem.broadcaster.lastBroadcast ?: error("Unexpected broadcast state")
        nameTextView.text = previousBroadcastItem.broadcaster.user.name
        lastBroadcastTextView.text = broadcast.dateText
        itemView.setOnClickListener {
            // TODO: show user's profile
        }
    }
}

class ListCard(view: View) : LobbyViewHolder(view) {
    private val recyclerView: RecyclerView = view.findViewById(R.id.card_list_recycler_view)
    private val snapHelper = LinearSnapHelper()
    init {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, RecyclerView.HORIZONTAL, false)
        snapHelper.attachToRecyclerView(recyclerView)
    }
    override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
        val item = item as LobbyItem.CardList
        recyclerView.adapter = LobbyAdapter(item.cards, tags, content)
    }
}


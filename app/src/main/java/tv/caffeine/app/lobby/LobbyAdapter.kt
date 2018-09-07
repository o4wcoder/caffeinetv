package tv.caffeine.app.lobby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.Api
import tv.caffeine.app.lobby.LobbyItem.Companion.CARD_LIST
import tv.caffeine.app.lobby.LobbyItem.Companion.HEADER
import tv.caffeine.app.lobby.LobbyItem.Companion.SINGLE_CARD
import tv.caffeine.app.lobby.LobbyItem.Companion.SUBTITLE

class LobbyAdapter(private val items: List<LobbyItem>,
                   private val tags: Map<String, Api.v3.Lobby.Tag>,
                   private val content: Map<String, Api.v3.Lobby.Content>
) : RecyclerView.Adapter<LobbyVH>() {

    override fun getItemCount(): Int = items.count()

    override fun getItemViewType(position: Int): Int = items[position].itemType()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyVH {
        val layout = when(viewType) {
            HEADER -> R.layout.lobby_header
            SUBTITLE -> R.layout.lobby_subtitle
            SINGLE_CARD -> R.layout.broadcast_card
            CARD_LIST -> R.layout.card_list
            else -> error("Unexpected view type")
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return when(viewType) {
            HEADER -> LobbyVH.HeaderCard(view)
            SUBTITLE -> LobbyVH.SubtitleCard(view)
            SINGLE_CARD -> LobbyVH.BroadcasterCard(view)
            CARD_LIST -> LobbyVH.ListCard(view)
            else -> error("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: LobbyVH, position: Int) {
        val item = items[position]
        holder.configure(item, tags, content)
    }

}

sealed class LobbyVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class HeaderCard(view: View) : LobbyVH(view) {
        private val headerTextView: TextView = view.findViewById(R.id.header_text_view)
        override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
            val item = item as LobbyItem.Header
            headerTextView.text = item.text
        }
    }

    class SubtitleCard(view: View) : LobbyVH(view) {
        private val subtitleTextView: TextView = view.findViewById(R.id.subtitle_text_view)
        override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
            val item = item as LobbyItem.Subtitle
            subtitleTextView.text = item.text
        }
    }

    class BroadcasterCard(view: View) : LobbyVH(view) {
        private val previewImageView: ImageView = view.findViewById(R.id.preview_image_view)
        private val avatarImageView: ImageView = view.findViewById(R.id.avatar_image_view)
        private val gameLogoImageView: ImageView = view.findViewById(R.id.game_logo_image_view)
        private val usernameTextView: TextView = view.findViewById(R.id.username_text_view)
        private val broadcastTitleTextView: TextView = view.findViewById(R.id.broadcast_title_text_view)
        private val tagTextView: TextView = view.findViewById(R.id.tag_text_view)
        private val friendsWatchingTextView: TextView = view.findViewById(R.id.friends_watching_text_view)

        override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
            val item = item as LobbyItem.SingleCard
            val broadcast = item.broadcaster.broadcast ?: item.broadcaster.lastBroadcast ?: error("Unexpected lobby item state")
            friendsWatchingTextView.isVisible = item.broadcaster.followingViewersCount > 0
            when(item.broadcaster.followingViewersCount) {
                0 -> friendsWatchingTextView.text = null
                1 -> friendsWatchingTextView.text =  itemView.context.getString(R.string.user_watching, item.broadcaster.followingViewers[0].username)
                else -> friendsWatchingTextView.text = itemView.context.resources.getQuantityString(R.plurals.user_and_friends_watching, item.broadcaster.followingViewersCount - 1, item.broadcaster.followingViewers[0].username, item.broadcaster.followingViewersCount - 1)
            }
            Picasso.get().load(broadcast.previewImagePath)
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
            val game = content[broadcast.contentId]
            if (game != null) {
                val gameLogoImageUrl = "https://images.caffeine.tv${game.iconImagePath}"
                Picasso.get()
                        .load(gameLogoImageUrl)
                        .into(gameLogoImageView)
            } else {
                gameLogoImageView.setImageDrawable(null)
            }
            broadcastTitleTextView.text = broadcast.name
            val tag = tags[item.broadcaster.tagId]
            tagTextView.isVisible = tag != null
            if (tag != null) {
                tagTextView.text = tag.name
                tagTextView.setTextColor(tag.color.toColorInt())
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

    class ListCard(view: View) : LobbyVH(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.card_list_recycler_view)
        private val snapHelper = LinearSnapHelper()
        init {
            recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, HORIZONTAL, false)
            snapHelper.attachToRecyclerView(recyclerView)
        }
        override fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>) {
            val item = item as LobbyItem.CardList
            recyclerView.adapter = LobbyAdapter(item.cards, tags, content)
        }
    }

    abstract fun configure(item: LobbyItem, tags: Map<String, Api.v3.Lobby.Tag>, content: Map<String, Api.v3.Lobby.Content>)
}

sealed class LobbyItem {
    class Header(val text: String) : LobbyItem()
    class Subtitle(val text: String) : LobbyItem()
    class SingleCard(val broadcaster: Api.v3.Lobby.Broadcaster) : LobbyItem()
//    class SingleCardV2(val title: String, val subtitle: String, val isLive: Boolean, val isFollowed: Boolean, val isFeatured: Boolean, val previewImageUrl: String, val avatarImageUrl: String, val contentLogoImageUrl: String) : LobbyItem()
    class CardList(val cards: List<SingleCard>) : LobbyItem()

    companion object {
        const val HEADER = 1
        const val SUBTITLE = 2
        const val SINGLE_CARD = 3
        const val CARD_LIST = 4

        fun parse(result: Api.v3.Lobby.Result): List<LobbyItem> {
            return result.sections.flatMap { section ->
                mutableListOf<LobbyItem>(Header(section.name)).apply {
                    section.emptyMessage?.let { add(Subtitle(it)) }
                    section.broadcasters?.map { SingleCard(it) }?.let { addAll(it) }
                    section.categories?.forEach { category ->
                        add(Subtitle(category.name))
                        add(CardList(category.broadcasters.map { SingleCard(it) }))
                    }
                }.toList()
            }
        }
    }

    fun itemType(): Int = when(this) {
        is LobbyItem.Header -> HEADER
        is LobbyItem.Subtitle -> SUBTITLE
        is LobbyItem.SingleCard -> SINGLE_CARD
        is LobbyItem.CardList -> CARD_LIST
    }
}

package tv.caffeine.app.lobby

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.di.ThemeFollowedLobby
import tv.caffeine.app.di.ThemeNotFollowedLobby
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import javax.inject.Inject

class LobbyAdapter @Inject constructor(
        private val followManager: FollowManager,
        internal val recycledViewPool: RecyclerView.RecycledViewPool,
        @ThemeFollowedLobby private val followedTheme: UserTheme,
        @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme
) : ListAdapter<LobbyItem, LobbyViewHolder>(
        object : DiffUtil.ItemCallback<LobbyItem>() {
            override fun areItemsTheSame(oldItem: LobbyItem, newItem: LobbyItem)
                    = oldItem.itemType == newItem.itemType && oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LobbyItem, newItem: LobbyItem) = oldItem == newItem
        }
) {
    private var tags: Map<String, Lobby.Tag> = mapOf()
    private var content: Map<String, Lobby.Content> = mapOf()

    fun submitList(list: List<LobbyItem>, tags: Map<String, Lobby.Tag>, content: Map<String, Lobby.Content>) {
        this.tags = tags
        this.content = content
        super.submitList(list)
    }

    override fun getItemViewType(position: Int): Int = getItem(position).itemType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyViewHolder {
        val itemType = LobbyItem.Type.values()[viewType]
        val layout = when(itemType) {
            LobbyItem.Type.HEADER -> R.layout.lobby_header
            LobbyItem.Type.SUBTITLE -> R.layout.lobby_subtitle
            LobbyItem.Type.LIVE_BROADCAST_CARD -> R.layout.live_broadcast_card
            LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> R.layout.previous_broadcast_card
            LobbyItem.Type.CARD_LIST -> R.layout.card_list
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return when(itemType) {
            LobbyItem.Type.HEADER -> HeaderCard(view)
            LobbyItem.Type.SUBTITLE -> SubtitleCard(view)
            LobbyItem.Type.LIVE_BROADCAST_CARD -> LiveBroadcastCard(view)
            LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> PreviousBroadcastCard(view)
            LobbyItem.Type.CARD_LIST -> ListCard(view, recycledViewPool)
        }
    }

    override fun onBindViewHolder(holder: LobbyViewHolder, position: Int) {
        val item = getItem(position)
        holder.configure(item, tags, content, followManager, followedTheme, notFollowedTheme)
    }

}

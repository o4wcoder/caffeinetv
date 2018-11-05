package tv.caffeine.app.lobby

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.*
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
        val inflater = LayoutInflater.from(parent.context)
        val itemType = LobbyItem.Type.values()[viewType]
        return when(itemType) {
            LobbyItem.Type.AVATAR_CARD -> AvatarCard(LobbyAvatarCardBinding.inflate(inflater, parent, false))
            LobbyItem.Type.HEADER -> HeaderCard(LobbyHeaderBinding.inflate(inflater, parent, false))
            LobbyItem.Type.SUBTITLE -> SubtitleCard(LobbySubtitleBinding.inflate(inflater, parent, false))
            LobbyItem.Type.LIVE_BROADCAST_CARD -> LiveBroadcastCard(LiveBroadcastCardBinding.inflate(inflater, parent, false))
            LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD -> LiveBroadcastWithFriendsCard(LiveBroadcastWithFriendsCardBinding.inflate(inflater, parent, false))
            LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> PreviousBroadcastCard(PreviousBroadcastCardBinding.inflate(inflater, parent, false))
            LobbyItem.Type.CARD_LIST -> ListCard(CardListBinding.inflate(inflater, parent, false), recycledViewPool)
        }
    }

    override fun onBindViewHolder(holder: LobbyViewHolder, position: Int) {
        val item = getItem(position)
        holder.configure(item, tags, content, followManager, followedTheme, notFollowedTheme)
    }

}

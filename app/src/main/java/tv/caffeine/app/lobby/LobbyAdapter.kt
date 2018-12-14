package tv.caffeine.app.lobby

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.*
import tv.caffeine.app.di.ThemeFollowedLobby
import tv.caffeine.app.di.ThemeFollowedLobbyLight
import tv.caffeine.app.di.ThemeNotFollowedLobby
import tv.caffeine.app.di.ThemeNotFollowedLobbyLight
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import javax.inject.Inject

class LobbyAdapter @Inject constructor(
        private val followManager: FollowManager,
        internal val recycledViewPool: RecyclerView.RecycledViewPool,
        @ThemeFollowedLobby private val followedTheme: UserTheme,
        @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme,
        @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
        @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme
) : ListAdapter<LobbyItem, LobbyViewHolder>(
        object : DiffUtil.ItemCallback<LobbyItem>() {
            override fun areItemsTheSame(oldItem: LobbyItem, newItem: LobbyItem)
                    = oldItem.itemType == newItem.itemType && oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LobbyItem, newItem: LobbyItem) = oldItem == newItem

            override fun getChangePayload(oldItem: LobbyItem, newItem: LobbyItem): Any? {
                return if (oldItem is CardList) CardList::class else null
            }
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
            LobbyItem.Type.AVATAR_CARD -> avatarCard(inflater, parent)
            LobbyItem.Type.HEADER -> headerCard(inflater, parent)
            LobbyItem.Type.SUBTITLE -> subtitleCard(inflater, parent)
            LobbyItem.Type.LIVE_BROADCAST_CARD -> liveBroadcastCard(inflater, parent)
            LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD -> liveBroadcastWithFriendsCard(inflater, parent)
            LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> previousBroadcastCard(inflater, parent)
            LobbyItem.Type.CARD_LIST -> listCard(inflater, parent)
        }
    }

    private fun avatarCard(inflater: LayoutInflater, parent: ViewGroup) =
            AvatarCard(LobbyAvatarCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight)

    private fun headerCard(inflater: LayoutInflater, parent: ViewGroup) =
            HeaderCard(LobbyHeaderBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight)

    private fun subtitleCard(inflater: LayoutInflater, parent: ViewGroup) =
            SubtitleCard(LobbySubtitleBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight)

    private fun liveBroadcastCard(inflater: LayoutInflater, parent: ViewGroup) =
            LiveBroadcastCard(LiveBroadcastCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight)

    private fun liveBroadcastWithFriendsCard(inflater: LayoutInflater, parent: ViewGroup) =
            LiveBroadcastWithFriendsCard(LiveBroadcastWithFriendsCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight)

    private fun previousBroadcastCard(inflater: LayoutInflater, parent: ViewGroup) =
            PreviousBroadcastCard(PreviousBroadcastCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight)

    private fun listCard(inflater: LayoutInflater, parent: ViewGroup) =
            ListCard(CardListBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, recycledViewPool)

    override fun onBindViewHolder(holder: LobbyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

}

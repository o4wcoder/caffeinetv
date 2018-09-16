package tv.caffeine.app.lobby

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.R
import tv.caffeine.app.api.Api
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.lobby.LobbyItem.Companion.CARD_LIST
import tv.caffeine.app.lobby.LobbyItem.Companion.HEADER
import tv.caffeine.app.lobby.LobbyItem.Companion.LIVE_BROADCAST_CARD
import tv.caffeine.app.lobby.LobbyItem.Companion.PREVIOUS_BROADCAST_CARD
import tv.caffeine.app.lobby.LobbyItem.Companion.SUBTITLE
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme

class LobbyAdapter(private val items: List<LobbyItem>,
                   private val tags: Map<String, Api.v3.Lobby.Tag>,
                   private val content: Map<String, Api.v3.Lobby.Content>,
                   private val followManager: FollowManager,
                   private val recycledViewPool: RecyclerView.RecycledViewPool,
                   @ThemeFollowedExplore private val followedTheme: UserTheme,
                   @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme
) : RecyclerView.Adapter<LobbyViewHolder>() {

    override fun getItemCount(): Int = items.count()

    override fun getItemViewType(position: Int): Int = items[position].itemType()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyViewHolder {
        val layout = when(viewType) {
            HEADER -> R.layout.lobby_header
            SUBTITLE -> R.layout.lobby_subtitle
            LIVE_BROADCAST_CARD -> R.layout.live_broadcast_card
            PREVIOUS_BROADCAST_CARD -> R.layout.previous_broadcast_card
            CARD_LIST -> R.layout.card_list
            else -> error("Unexpected view type")
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return when(viewType) {
            HEADER -> HeaderCard(view)
            SUBTITLE -> SubtitleCard(view)
            LIVE_BROADCAST_CARD -> LiveBroadcastCard(view)
            PREVIOUS_BROADCAST_CARD -> PreviousBroadcastCard(view)
            CARD_LIST -> ListCard(view, recycledViewPool)
            else -> error("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: LobbyViewHolder, position: Int) {
        val item = items[position]
        holder.configure(item, tags, content, followManager, followedTheme, notFollowedTheme)
    }

}

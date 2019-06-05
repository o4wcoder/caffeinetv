package tv.caffeine.app.lobby

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.threeten.bp.Clock
import timber.log.Timber
import tv.caffeine.app.analytics.EventManager
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.CardListBinding
import tv.caffeine.app.databinding.LiveBroadcastCardBinding
import tv.caffeine.app.databinding.LiveBroadcastWithFriendsCardBinding
import tv.caffeine.app.databinding.LobbyAvatarCardBinding
import tv.caffeine.app.databinding.LobbyFollowPeopleCardBinding
import tv.caffeine.app.databinding.LobbyHeaderBinding
import tv.caffeine.app.databinding.LobbySubtitleBinding
import tv.caffeine.app.databinding.PreviousBroadcastCardBinding
import tv.caffeine.app.databinding.UpcomingButtonCardBinding
import tv.caffeine.app.di.ThemeFollowedLobby
import tv.caffeine.app.di.ThemeFollowedLobbyLight
import tv.caffeine.app.di.ThemeNotFollowedLobby
import tv.caffeine.app.di.ThemeNotFollowedLobbyLight
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class LobbyAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    private val followManager: FollowManager,
    internal val recycledViewPool: RecyclerView.RecycledViewPool,
    @ThemeFollowedLobby private val followedTheme: UserTheme,
    @ThemeNotFollowedLobby private val notFollowedTheme: UserTheme,
    @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
    @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme,
    private val picasso: Picasso,
    private val clock: Clock,
    private val eventManager: EventManager
) : ListAdapter<LobbyItem, LobbyViewHolder>(
        object : DiffUtil.ItemCallback<LobbyItem>() {
            override fun areItemsTheSame(oldItem: LobbyItem, newItem: LobbyItem) =
                    oldItem.itemType == newItem.itemType && oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LobbyItem, newItem: LobbyItem) = oldItem == newItem

            override fun getChangePayload(oldItem: LobbyItem, newItem: LobbyItem): Any? {
                return if (oldItem is CardList) CardList::class else null
            }
        }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    private var tags: Map<String, Lobby.Tag> = mapOf()
    private var content: Map<String, Lobby.Content> = mapOf()
    private var payloadId: String? = null
    var isMiniStyle: Boolean = false

    fun submitList(
        list: List<LobbyItem>,
        tags: Map<String, Lobby.Tag>,
        content: Map<String, Lobby.Content>,
        payloadId: String
    ) {
        this.tags = tags
        this.content = content
        this.payloadId = payloadId
        super.submitList(list)
    }

    override fun getItemViewType(position: Int): Int = getItem(position).itemType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemType = LobbyItem.Type.values()[viewType]
        return when (itemType) {
            LobbyItem.Type.AVATAR_CARD -> avatarCard(inflater, parent)
            LobbyItem.Type.FOLLOW_PEOPLE_CARD -> followPeopleCard(inflater, parent)
            LobbyItem.Type.HEADER -> headerCard(inflater, parent)
            LobbyItem.Type.SUBTITLE -> subtitleCard(inflater, parent)
            LobbyItem.Type.LIVE_BROADCAST_CARD -> liveBroadcastCard(inflater, parent)
            LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD -> liveBroadcastWithFriendsCard(inflater, parent)
            LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> previousBroadcastCard(inflater, parent)
            LobbyItem.Type.CARD_LIST -> listCard(inflater, parent)
            LobbyItem.Type.UPCOMING_BUTTON_CARD -> upcomingButtonCard(inflater, parent)
        }
    }

    private fun avatarCard(inflater: LayoutInflater, parent: ViewGroup) =
            AvatarCard(LobbyAvatarCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso)

    private fun followPeopleCard(inflater: LayoutInflater, parent: ViewGroup) =
            FollowPeopleCard(LobbyFollowPeopleCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso)

    private fun headerCard(inflater: LayoutInflater, parent: ViewGroup) =
            HeaderCard(LobbyHeaderBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso)

    private fun subtitleCard(inflater: LayoutInflater, parent: ViewGroup) =
            SubtitleCard(LobbySubtitleBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso)

    private fun liveBroadcastCard(inflater: LayoutInflater, parent: ViewGroup) =
            LiveBroadcastCard(LiveBroadcastCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, payloadId, this, clock, eventManager).apply {
                binding.isMiniStyle = isMiniStyle
            }

    private fun liveBroadcastWithFriendsCard(inflater: LayoutInflater, parent: ViewGroup) =
            LiveBroadcastWithFriendsCard(LiveBroadcastWithFriendsCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, payloadId, this, clock, eventManager)

    private fun previousBroadcastCard(inflater: LayoutInflater, parent: ViewGroup) =
            PreviousBroadcastCard(PreviousBroadcastCardBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, payloadId, this, clock, eventManager)

    private fun listCard(inflater: LayoutInflater, parent: ViewGroup) =
            ListCard(CardListBinding.inflate(inflater, parent, false), tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, payloadId, recycledViewPool, dispatchConfig, clock, eventManager)

    private fun upcomingButtonCard(inflater: LayoutInflater, parent: ViewGroup) =
            UpcomingButtonCard(UpcomingButtonCardBinding.inflate(inflater, parent, false), null, tags, content, followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso)

    override fun onBindViewHolder(holder: LobbyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

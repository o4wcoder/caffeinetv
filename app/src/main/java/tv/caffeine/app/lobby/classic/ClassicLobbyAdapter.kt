package tv.caffeine.app.lobby.classic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
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
import tv.caffeine.app.lobby.CardList
import tv.caffeine.app.lobby.LobbyItem
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ClassicLobbyAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    private val liveBroadcastCardFactory: LiveBroadcastCard.Factory,
    private val liveBroadcastWithFriendsCardFactory: LiveBroadcastWithFriendsCard.Factory,
    private val previousBroadcastCardFactory: PreviousBroadcastCard.Factory,
    private val listCardFactory: ListCard.Factory
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
        AvatarCard(
            LobbyAvatarCardBinding.inflate(
                inflater,
                parent,
                false
            )
        )

    private fun followPeopleCard(inflater: LayoutInflater, parent: ViewGroup) =
        FollowPeopleCard(
            LobbyFollowPeopleCardBinding.inflate(
                inflater,
                parent,
                false
            )
        )

    private fun headerCard(inflater: LayoutInflater, parent: ViewGroup) =
        HeaderCard(
            LobbyHeaderBinding.inflate(
                inflater,
                parent,
                false
            )
        )

    private fun subtitleCard(inflater: LayoutInflater, parent: ViewGroup) =
        SubtitleCard(
            LobbySubtitleBinding.inflate(
                inflater,
                parent,
                false
            )
        )

    private fun liveBroadcastCard(inflater: LayoutInflater, parent: ViewGroup) =
            liveBroadcastCardFactory.create(
                LiveBroadcastCardBinding.inflate(inflater, parent, false).apply { isMiniStyle = this@ClassicLobbyAdapter.isMiniStyle },
                tags,
                content,
                payloadId,
                this
            )

    private fun liveBroadcastWithFriendsCard(inflater: LayoutInflater, parent: ViewGroup) =
            liveBroadcastWithFriendsCardFactory.create(
                LiveBroadcastWithFriendsCardBinding.inflate(inflater, parent, false),
                tags,
                content,
                payloadId,
                this
            )

    private fun previousBroadcastCard(inflater: LayoutInflater, parent: ViewGroup) =
            previousBroadcastCardFactory.create(
                PreviousBroadcastCardBinding.inflate(inflater, parent, false),
                tags,
                content,
                payloadId,
                this
            )

    private fun listCard(inflater: LayoutInflater, parent: ViewGroup) =
            listCardFactory.create(CardListBinding.inflate(inflater, parent, false), tags, content, payloadId)

    private fun upcomingButtonCard(inflater: LayoutInflater, parent: ViewGroup) =
        UpcomingButtonCard(
            UpcomingButtonCardBinding.inflate(
                inflater,
                parent,
                false
            ), null
        )

    override fun onBindViewHolder(holder: LobbyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

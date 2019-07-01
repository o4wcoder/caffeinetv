package tv.caffeine.app.lobby.release

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import tv.caffeine.app.analytics.LobbyImpressionAnalytics
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.CardListBinding
import tv.caffeine.app.databinding.ReleaseUiHeaderBinding
import tv.caffeine.app.databinding.ReleaseUiOnlineBroadcasterCardBinding
import tv.caffeine.app.databinding.ReleaseUiSubtitleBinding
import tv.caffeine.app.lobby.CardList
import tv.caffeine.app.lobby.GenericLobbyAdapter
import tv.caffeine.app.lobby.Header
import tv.caffeine.app.lobby.LiveBroadcast
import tv.caffeine.app.lobby.LiveBroadcastWithFriends
import tv.caffeine.app.lobby.LobbyItem
import tv.caffeine.app.lobby.Subtitle
import tv.caffeine.app.util.safeNavigate
import kotlin.coroutines.CoroutineContext

class ReleaseLobbyAdapter @AssistedInject constructor(
    private val onlineBroadcasterFactory: OnlineBroadcaster.Factory,
    private val horizontalScrollCardFactory: HorizontalScrollCard.Factory,
    private val lobbyImpressionAnalyticsFactory: LobbyImpressionAnalytics.Factory,
    private val largeOnlineBroadcasterCardFactory: LargeOnlineBroadcasterCard.Factory,
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted private val navController: NavController
) : GenericLobbyAdapter<CardViewHolder>(DiffCallback()), CoroutineScope {

    class DiffCallback : DiffUtil.ItemCallback<LobbyItem>() {
        override fun areItemsTheSame(oldItem: LobbyItem, newItem: LobbyItem) =
            oldItem.itemType == newItem.itemType && oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LobbyItem, newItem: LobbyItem) = oldItem == newItem

        override fun getChangePayload(oldItem: LobbyItem, newItem: LobbyItem): Any? {
            return if (oldItem is CardList) CardList::class else null
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(lifecycleOwner: LifecycleOwner, navController: NavController): ReleaseLobbyAdapter
    }

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job + exceptionHandler

    fun <T> LiveData<Event<T>>.observeEvents(lifecycleOwner: LifecycleOwner, block: (T) -> Unit) {
        observe(lifecycleOwner, Observer<Event<T>> { event ->
            event.getContentIfNotHandled()?.let { block(it) }
        })
    }

    private var tags: Map<String, Lobby.Tag> = mapOf()
    private var content: Map<String, Lobby.Content> = mapOf()
    private var payloadId: String? = null
    private lateinit var lobbyImpressionAnalytics: LobbyImpressionAnalytics

    override fun submitList(
        list: List<LobbyItem>,
        tags: Map<String, Lobby.Tag>,
        content: Map<String, Lobby.Content>,
        payloadId: String
    ) {
        super.submitList(list)
        this.tags = tags
        this.content = content
        this.payloadId = payloadId
        lobbyImpressionAnalytics = lobbyImpressionAnalyticsFactory.create(payloadId)
    }

    override fun getItemViewType(position: Int): Int = getItem(position).itemType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemType = LobbyItem.Type.values()[viewType]
        return when (itemType) {
            // LobbyItem.Type.AVATAR_CARD -> avatarCard(inflater, parent)
            // LobbyItem.Type.FOLLOW_PEOPLE_CARD -> followPeopleCard(inflater, parent)
            LobbyItem.Type.HEADER -> headerCard(inflater, parent)
            LobbyItem.Type.SUBTITLE -> subtitleCard(inflater, parent)
            LobbyItem.Type.LIVE_BROADCAST_CARD -> largeOnlineBroadcasterCard(inflater, parent)
            LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD -> largeOnlineBroadcasterCard(inflater, parent)
            // LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> previousBroadcastCard(inflater, parent)
            LobbyItem.Type.CARD_LIST -> listCard(inflater, parent)
            // LobbyItem.Type.UPCOMING_BUTTON_CARD -> upcomingButtonCard(inflater, parent)
            else -> TextCard(TextView(parent.context))
        }
    }

    private fun headerCard(inflater: LayoutInflater, parent: ViewGroup) =
            ReleaseHeaderCard(
                ReleaseUiHeaderBinding.inflate(inflater, parent, false)
            )

    private fun subtitleCard(inflater: LayoutInflater, parent: ViewGroup) =
            ReleaseSubtitleCard(
                ReleaseUiSubtitleBinding.inflate(inflater, parent, false)
            )

    private fun largeOnlineBroadcasterCard(inflater: LayoutInflater, parent: ViewGroup) =
            largeOnlineBroadcasterCardFactory.create(
                ReleaseUiOnlineBroadcasterCardBinding.inflate(inflater, parent, false),
                this
            )

    private fun listCard(inflater: LayoutInflater, parent: ViewGroup) =
        horizontalScrollCardFactory.create(
            CardListBinding.inflate(inflater, parent, false),
            tags,
            content,
            payloadId,
            lifecycleOwner,
            navController
        )

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ReleaseHeaderCard -> bind(holder, item)
            is ReleaseSubtitleCard -> bind(holder, item)
            is LargeOnlineBroadcasterCard -> bind(holder, item)
            is HorizontalScrollCard -> bind(holder, item)
        }
    }

    private fun bind(headerCard: ReleaseHeaderCard, item: LobbyItem) {
        val header = item as Header
        headerCard.bind(header)
    }

    private fun bind(subtitleCard: ReleaseSubtitleCard, item: LobbyItem) {
        val subtitle = item as Subtitle
        subtitleCard.bind(subtitle)
    }

    private fun bind(largeOnlineBroadcasterCard: LargeOnlineBroadcasterCard, item: LobbyItem) {
        val liveBroadcast = when (item) {
            is LiveBroadcast -> item
            is LiveBroadcastWithFriends -> LiveBroadcast(item.id, item.broadcaster)
            else -> return
        }
        val onlineBroadcaster = onlineBroadcasterFactory.create(liveBroadcast, lobbyImpressionAnalytics, this)
        largeOnlineBroadcasterCard.bind(onlineBroadcaster)
        onlineBroadcaster.navigationCommands.observeEvents(lifecycleOwner) {
            when (it) {
                is NavigationCommand.To -> navController.safeNavigate(it.directions)
            }
        }
    }

    private fun bind(horizontalScrollCard: HorizontalScrollCard, item: LobbyItem) {
        val cardList = item as CardList
        horizontalScrollCard.bind(cardList)
    }
}

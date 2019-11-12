package tv.caffeine.app.lobby.release

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import tv.caffeine.app.databinding.ReleaseUiDoubleCategoryCardBinding
import tv.caffeine.app.databinding.ReleaseUiEmptyJoinYourFriendsCardBinding
import tv.caffeine.app.databinding.ReleaseUiHeaderBinding
import tv.caffeine.app.databinding.ReleaseUiOfflineBroadcasterCardBinding
import tv.caffeine.app.databinding.ReleaseUiOnlineBroadcasterCardBinding
import tv.caffeine.app.databinding.ReleaseUiSingleCategoryCardBinding
import tv.caffeine.app.databinding.ReleaseUiSubtitleBinding
import tv.caffeine.app.lobby.CardList
import tv.caffeine.app.lobby.DoubleCategory
import tv.caffeine.app.lobby.GenericLobbyAdapter
import tv.caffeine.app.lobby.Header
import tv.caffeine.app.lobby.LiveBroadcast
import tv.caffeine.app.lobby.LiveBroadcastWithFriends
import tv.caffeine.app.lobby.LobbyItem
import tv.caffeine.app.lobby.PreviousBroadcast
import tv.caffeine.app.lobby.SingleCategory
import tv.caffeine.app.lobby.Subtitle
import tv.caffeine.app.lobby.toDistinctLiveBroadcasters
import tv.caffeine.app.util.safeNavigate
import kotlin.coroutines.CoroutineContext

class ReleaseLobbyAdapter @AssistedInject constructor(
    private val onlineBroadcasterFactory: OnlineBroadcaster.Factory,
    private val offlineBroadcasterFactory: OfflineBroadcaster.Factory,
    private val horizontalScrollCardFactory: HorizontalScrollCard.Factory,
    private val lobbyImpressionAnalyticsFactory: LobbyImpressionAnalytics.Factory,
    private val largeOnlineBroadcasterCardFactory: LargeOnlineBroadcasterCard.Factory,
    private val categoryCardViewModelFactory: CategoryCardViewModel.Factory,
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted private val navController: NavController
) : GenericLobbyAdapter<CardViewHolder>(DiffCallback()), CoroutineScope {

    var isMiniStyle = false

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

    private var tags: Map<String, Lobby.Tag> = mapOf()
    private var content: Map<String, Lobby.Content> = mapOf()
    private var payloadId: String? = null
    private var allDistinctLiveBroadcasters: List<String>? = null
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
        this.allDistinctLiveBroadcasters = list.toDistinctLiveBroadcasters()
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
            LobbyItem.Type.LIVE_BROADCAST_CARD -> largeOnlineBroadcasterCard(inflater, parent, isMiniStyle)
            LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD -> largeOnlineBroadcasterCard(inflater, parent, isMiniStyle)
            LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> offlineBroadcasterCard(inflater, parent)
            LobbyItem.Type.CARD_LIST -> listCard(inflater, parent)
            // LobbyItem.Type.UPCOMING_BUTTON_CARD -> upcomingButtonCard(inflater, parent)
            LobbyItem.Type.SINGLE_CATEGORY_CARD -> singleCategoryCard(inflater, parent, isMiniStyle)
            LobbyItem.Type.DOUBLE_CATEGORY_CARD -> doubleCategoryCard(inflater, parent)
            LobbyItem.Type.CATEGORY_CARD_LIST -> listCard(inflater, parent)
            LobbyItem.Type.EMPTY_JOIN_YOUR_FRIENDS -> emptyJoinYourFriendsCard(inflater, parent)
            else -> EmptyCard(View(parent.context))
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

    private fun largeOnlineBroadcasterCard(inflater: LayoutInflater, parent: ViewGroup, isMiniStyle: Boolean) =
            largeOnlineBroadcasterCardFactory.create(
                ReleaseUiOnlineBroadcasterCardBinding.inflate(inflater, parent, false).apply {
                    this.isMiniStyle = isMiniStyle
                },
                this,
                lifecycleOwner
            )

    private fun offlineBroadcasterCard(inflater: LayoutInflater, parent: ViewGroup) =
            OfflineBroadcasterCard(
                ReleaseUiOfflineBroadcasterCardBinding.inflate(inflater, parent, false),
                lifecycleOwner
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

    private fun singleCategoryCard(inflater: LayoutInflater, parent: ViewGroup, isMiniStyle: Boolean) =
        SingleCategoryCard(ReleaseUiSingleCategoryCardBinding.inflate(inflater, parent, false).apply {
            this.isMiniStyle = isMiniStyle
        })

    private fun doubleCategoryCard(inflater: LayoutInflater, parent: ViewGroup) =
        DoubleCategoryCard(ReleaseUiDoubleCategoryCardBinding.inflate(inflater, parent, false))

    private fun emptyJoinYourFriendsCard(inflater: LayoutInflater, parent: ViewGroup) =
        EmptyJoinYourFriendsCard(ReleaseUiEmptyJoinYourFriendsCardBinding.inflate(inflater, parent, false))

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = getItem(position)
        holder.setItemViewType(item)
        when (holder) {
            is ReleaseHeaderCard -> bind(holder, item)
            is ReleaseSubtitleCard -> bind(holder, item)
            is LargeOnlineBroadcasterCard -> bind(holder, item, allDistinctLiveBroadcasters)
            is OfflineBroadcasterCard -> bind(holder, item)
            is HorizontalScrollCard -> bind(holder, item)
            is SingleCategoryCard -> bind(holder, item)
            is DoubleCategoryCard -> bind(holder, item)
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

    private fun bind(
        largeOnlineBroadcasterCard: LargeOnlineBroadcasterCard,
        item: LobbyItem,
        allDistinctLiveBroadcasters: List<String>?
    ) {
        val liveBroadcast = when (item) {
            is LiveBroadcast -> item
            is LiveBroadcastWithFriends -> LiveBroadcast(item.id, item.broadcaster)
            else -> return
        }
        val onlineBroadcaster = onlineBroadcasterFactory.create(
            liveBroadcast.broadcaster, lobbyImpressionAnalytics, this, allDistinctLiveBroadcasters)
        largeOnlineBroadcasterCard.bind(onlineBroadcaster)
        onlineBroadcaster.navigationCommands.observeEvents(lifecycleOwner) {
            when (it) {
                is NavigationCommand.To -> navController.safeNavigate(it.directions)
            }
        }
    }

    private fun bind(offlineBroadcasterCard: OfflineBroadcasterCard, item: LobbyItem) {
        val offlineBroadcast = item as PreviousBroadcast
        val offlineBroadcaster = offlineBroadcasterFactory.create(offlineBroadcast.broadcaster, lobbyImpressionAnalytics, this)
        offlineBroadcasterCard.bind(offlineBroadcaster)
        offlineBroadcaster.navigationCommands.observeEvents(lifecycleOwner) {
            when (it) {
                is NavigationCommand.To -> navController.safeNavigate(it.directions)
            }
        }
    }

    private fun bind(horizontalScrollCard: HorizontalScrollCard, item: LobbyItem) {
        horizontalScrollCard.bind(item)
    }

    private fun bind(singleCategoryCard: SingleCategoryCard, item: LobbyItem) {
        val card = (item as SingleCategory).categoryCard
        val context = singleCategoryCard.itemView.context
        val categoryViewModel = categoryCardViewModelFactory.create(card, context)
        singleCategoryCard.bind(categoryViewModel)
        categoryViewModel.navigationCommands.observeEvents(lifecycleOwner) {
            when (it) {
                is NavigationCommand.To -> navController.safeNavigate(it.directions)
            }
        }
    }

    private fun bind(doubleCategoryCard: DoubleCategoryCard, item: LobbyItem) {
        val cardList = (item as DoubleCategory).categoryCardList.categoryCards
        val context = doubleCategoryCard.itemView.context
        val categoryViewModels = listOf(
            categoryCardViewModelFactory.create(cardList[0], context),
            categoryCardViewModelFactory.create(cardList[1], context))
        doubleCategoryCard.bind(categoryViewModels)
        for (categoryViewModel in categoryViewModels) {
            categoryViewModel.navigationCommands.observeEvents(lifecycleOwner) {
                when (it) {
                    is NavigationCommand.To -> navController.safeNavigate(it.directions)
                }
            }
        }
    }
}

fun <T> LiveData<Event<T>>.observeEvents(lifecycleOwner: LifecycleOwner, block: (T) -> Unit) {
    observe(lifecycleOwner, Observer<Event<T>> { event ->
        event.getContentIfNotHandled()?.let { block(it) }
    })
}

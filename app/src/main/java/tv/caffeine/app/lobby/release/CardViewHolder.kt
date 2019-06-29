package tv.caffeine.app.lobby.release

import android.graphics.Rect
import android.view.View
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.CardListBinding
import tv.caffeine.app.databinding.ReleaseUiHeaderBinding
import tv.caffeine.app.databinding.ReleaseUiOnlineBroadcasterCardBinding
import tv.caffeine.app.databinding.ReleaseUiSubtitleBinding
import tv.caffeine.app.lobby.CardList
import tv.caffeine.app.lobby.Header
import tv.caffeine.app.lobby.LobbyItem
import tv.caffeine.app.lobby.Subtitle

sealed class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class ReleaseHeaderCard(
    private val viewBinding: ReleaseUiHeaderBinding
) : CardViewHolder(viewBinding.root) {

    fun bind(header: Header) {
        viewBinding.viewModel = header
    }
}

class ReleaseSubtitleCard(
    private val viewBinding: ReleaseUiSubtitleBinding
) : CardViewHolder(viewBinding.root) {

    fun bind(subtitle: Subtitle) {
        viewBinding.viewModel = subtitle
    }
}

class LargeOnlineBroadcasterCard(
    private val viewBinding: ReleaseUiOnlineBroadcasterCardBinding
) : CardViewHolder(viewBinding.root) {

    fun bind(onlineBroadcaster: OnlineBroadcaster) {
        viewBinding.viewModel = onlineBroadcaster
    }
}

class HorizontalScrollCard @AssistedInject constructor(
    @Assisted val binding: CardListBinding,
    @Assisted private val tags: Map<String, Lobby.Tag>,
    @Assisted private val content: Map<String, Lobby.Content>,
    @Assisted private val payloadId: String?,
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted private val navController: NavController,
    recycledViewPool: RecyclerView.RecycledViewPool,
    lobbyAdapterFactory: ReleaseLobbyAdapter.Factory
) : CardViewHolder(binding.root) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            binding: CardListBinding,
            tags: Map<String, Lobby.Tag>,
            content: Map<String, Lobby.Content>,
            payloadId: String?,
            lifecycleOwner: LifecycleOwner,
            navController: NavController
        ): HorizontalScrollCard
    }

    private val snapHelper = LinearSnapHelper()
    private val edgeOffset = binding.root.resources.getDimension(R.dimen.lobby_card_side_margin).toInt()
    private val insetOffset = binding.root.resources.getDimension(R.dimen.lobby_card_narrow_margin).toInt()
    private val lobbyAdapter = lobbyAdapterFactory.create(lifecycleOwner, navController)

    init {
        binding.cardListRecyclerView.adapter = lobbyAdapter
        binding.cardListRecyclerView.run {
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
                    when (itemPosition) {
                        0 -> outRect.set(edgeOffset, 0, insetOffset, 0)
                        (adapter?.itemCount ?: 0) - 1 -> outRect.set(insetOffset, 0, edgeOffset, 0)
                        else -> outRect.set(insetOffset, 0, insetOffset, 0)
                    }
                }
            })
            setRecycledViewPool(recycledViewPool)
        }
        snapHelper.attachToRecyclerView(binding.cardListRecyclerView)
        itemView.updateLayoutParams<RecyclerView.LayoutParams> {
            marginStart = 0
            marginEnd = 0
        }
    }

    fun bind(cardList: CardList) {
        if (payloadId != null) {
            lobbyAdapter.submitList(cardList.cards, tags, content, payloadId)
        }
    }
}

class TextCard(itemView: View) : CardViewHolder(itemView) {
    fun bind(lobbyItem: LobbyItem) {
    }
}

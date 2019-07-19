package tv.caffeine.app.lobby.release

import android.graphics.Rect
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.webrtc.EglRenderer
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.CardListBinding
import tv.caffeine.app.databinding.ReleaseUiHeaderBinding
import tv.caffeine.app.databinding.ReleaseUiOfflineBroadcasterCardBinding
import tv.caffeine.app.databinding.ReleaseUiOnlineBroadcasterCardBinding
import tv.caffeine.app.databinding.ReleaseUiSubtitleBinding
import tv.caffeine.app.lobby.CardList
import tv.caffeine.app.lobby.Header
import tv.caffeine.app.lobby.LiveInTheLobbyCapable
import tv.caffeine.app.lobby.LobbyItem
import tv.caffeine.app.lobby.Subtitle
import tv.caffeine.app.settings.AutoPlayConfig
import tv.caffeine.app.stage.NewReyesController
import tv.caffeine.app.util.fadeOut
import tv.caffeine.app.webrtc.SurfaceViewRendererTuner

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

class LargeOnlineBroadcasterCard @AssistedInject constructor(
    @Assisted private val binding: ReleaseUiOnlineBroadcasterCardBinding,
    @Assisted private val scope: CoroutineScope,
    private val stageControllerFactory: NewReyesController.Factory,
    private val surfaceViewRendererTuner: SurfaceViewRendererTuner,
    private val autoPlayConfig: AutoPlayConfig
) : CardViewHolder(binding.root), LiveInTheLobbyCapable {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            binding: ReleaseUiOnlineBroadcasterCardBinding,
            scope: CoroutineScope
        ): LargeOnlineBroadcasterCard
    }

    fun bind(onlineBroadcaster: OnlineBroadcaster) {
        turnOffLiveVideo()
        binding.viewModel = onlineBroadcaster
        turnOnLiveVideo(onlineBroadcaster.broadcaster)
    }

    override var frameListener: EglRenderer.FrameListener? = null

    var stageController: NewReyesController? = null

    fun turnOffLiveVideo() {
        stageController?.close()
        stageController = null
        binding.previewImageView.isInvisible = false
        binding.primaryViewRenderer.release()
    }

    private fun turnOnLiveVideo(broadcaster: Lobby.Broadcaster) {
        if (autoPlayConfig.isAutoPlayEnabled(broadcaster.displayOrder)) {
            surfaceViewRendererTuner.configure(binding.primaryViewRenderer)
            val renderer = binding.primaryViewRenderer
            val username = broadcaster.user.username
            val controller = stageControllerFactory.create(username, true)
            controller.connect()
            stageController = controller
            scope.launch {
                startLiveVideo(renderer, controller) {
                    binding.previewImageView.fadeOut()
                }
            }
        }
    }
}

class OfflineBroadcasterCard(val binding: ReleaseUiOfflineBroadcasterCardBinding) : CardViewHolder(binding.root) {
    fun bind(offlineBroadcaster: OfflineBroadcaster) {
        binding.viewModel = offlineBroadcaster
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

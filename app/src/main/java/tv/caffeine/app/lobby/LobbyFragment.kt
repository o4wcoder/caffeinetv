package tv.caffeine.app.lobby

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentLobbyBinding
import tv.caffeine.app.ui.CaffeineFragment
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LobbyFragment @Inject constructor(
    private val lobbyAdapter: LobbyAdapter
) : CaffeineFragment(R.layout.fragment_lobby) {

    private val viewModel: LobbyViewModel by viewModels { viewModelFactory }
    private var binding: FragmentLobbyBinding? = null
    private var refreshJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLobbyBinding.bind(view)
        configure(binding)
        this.binding = binding
    }

    private fun configure(binding: FragmentLobbyBinding) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.lobbyRecyclerView.run {
            adapter = lobbyAdapter
            addItemDecoration(itemDecorator)
        }
        binding.lobbySwipeRefreshLayout.setOnRefreshListener { refreshLobby() }
        viewModel.lobby.observe(viewLifecycleOwner, Observer { result ->
            binding.lobbySwipeRefreshLayout.isRefreshing = false
            handle(result) { lobby ->
                val items = LobbyItem.parse(lobby)
                lobbyAdapter.submitList(items, lobby.tags, lobby.content)
                binding.lobbyLoadingIndicator.isVisible = false
            }
        })
    }

    override fun onDestroyView() {
        binding?.lobbyRecyclerView?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun refreshLobby() {
        refreshJob?.cancel()
        refreshJob = launch {
            while (isActive) {
                viewModel.refresh()
                delay(TimeUnit.SECONDS.toMillis(30))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshLobby()
    }

    override fun onStop() {
        super.onStop()
        refreshJob?.cancel()
        refreshJob = null
    }

    private val edgeOffset by lazy { resources.getDimensionPixelSize(R.dimen.lobby_card_side_margin) }
    private val listTopBottomOffset by lazy { resources.getDimensionPixelSize(R.dimen.lobby_list_top_bottom_margin) }
    private val cardSpacing by lazy { resources.getDimensionPixelSize(R.dimen.lobby_card_vertical_spacing) }
    private val headerTopMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_header_top_margin) }
    private val headerBottomMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_header_bottom_margin) }
    private val subtitleTopMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_subtitle_top_margin) }
    private val subtitleBottomMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_subtitle_bottom_margin) }

    private val itemDecorator = object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val itemType = view.tag as? LobbyItem.Type ?: return
            val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
            val extraTopOffset = if (itemPosition == 0) listTopBottomOffset else 0
            val extraBottomOffset = if (itemPosition == lobbyAdapter.itemCount - 1) listTopBottomOffset else 0
            when (itemType) {
                LobbyItem.Type.AVATAR_CARD -> outRect.set(edgeOffset, extraTopOffset + cardSpacing, edgeOffset, extraBottomOffset + cardSpacing)
                // FOLLOW_PEOPLE_CARD is the last special card. Do not include cardSpacing as the bottom margin.
                LobbyItem.Type.FOLLOW_PEOPLE_CARD -> outRect.set(edgeOffset, extraTopOffset + cardSpacing, edgeOffset, extraBottomOffset)
                LobbyItem.Type.LIVE_BROADCAST_CARD, LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD, LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> outRect.set(edgeOffset, extraTopOffset + cardSpacing, edgeOffset, extraBottomOffset + cardSpacing)
                LobbyItem.Type.CARD_LIST -> outRect.set(0, extraTopOffset + cardSpacing, 0, extraBottomOffset + cardSpacing)
                LobbyItem.Type.HEADER -> outRect.set(edgeOffset, extraTopOffset + headerTopMargin, edgeOffset, extraBottomOffset + headerBottomMargin)
                LobbyItem.Type.SUBTITLE -> outRect.set(edgeOffset, extraTopOffset + subtitleTopMargin, edgeOffset, extraBottomOffset + subtitleBottomMargin)
            }
        }
    }
}

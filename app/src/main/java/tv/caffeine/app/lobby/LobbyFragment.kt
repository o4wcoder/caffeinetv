package tv.caffeine.app.lobby

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.databinding.FragmentLobbyBinding
import tv.caffeine.app.lobby.classic.ClassicLobbyAdapter
import tv.caffeine.app.lobby.classic.LobbyViewHolder
import tv.caffeine.app.lobby.release.LargeOnlineBroadcasterCard
import tv.caffeine.app.lobby.release.ReleaseLobbyAdapter
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.fadeOutLoadingIndicator
import tv.caffeine.app.util.navigateToSendingVerificationEmailDialog
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

abstract class GenericLobbyAdapter<VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<LobbyItem>
) : ListAdapter<LobbyItem, VH>(diffCallback) {
    abstract fun submitList(
        list: List<LobbyItem>,
        tags: Map<String, Lobby.Tag>,
        content: Map<String, Lobby.Content>,
        payloadId: String
    )
}

class LobbyFragment @Inject constructor(
    private val releaseDesignConfig: ReleaseDesignConfig,
    private val classicLobbyAdapterProvider: Provider<ClassicLobbyAdapter>,
    private val releaseLobbyAdapterFactoryProvider: Provider<ReleaseLobbyAdapter.Factory>
) : CaffeineFragment(R.layout.fragment_lobby) {

    private val viewModel: LobbyViewModel by viewModels { viewModelFactory }
    private var binding: FragmentLobbyBinding? = null
    private var refreshJob: Job? = null
    private lateinit var lobbyAdapter: GenericLobbyAdapter<*>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLobbyBinding.bind(view)
        configure(binding)
        this.binding = binding
    }

    private fun configure(binding: FragmentLobbyBinding) {
        val isReleaseDesign = releaseDesignConfig.isReleaseDesignActive()
        lobbyAdapter = if (isReleaseDesign) {
            releaseLobbyAdapterFactoryProvider.get().create(viewLifecycleOwner, findNavController())
        } else {
            classicLobbyAdapterProvider.get()
        }
        val itemDecorator = if (isReleaseDesign) {
            ReleaseLobbyItemDecoration(resources)
        } else {
            ClassicLobbyItemDecoration(resources, lobbyAdapter)
        }
        binding.lifecycleOwner = viewLifecycleOwner
        binding.lobbyRecyclerView.run {
            adapter = lobbyAdapter
            addItemDecoration(itemDecorator)
        }
        binding.lobbySwipeRefreshLayout.setOnRefreshListener { refreshLobby() }
        binding.lobbyRecyclerView.setRecyclerListener { viewHolder ->
            when (viewHolder) {
                is LobbyViewHolder -> viewHolder.recycle()
                is LargeOnlineBroadcasterCard -> viewHolder.turnOffLiveVideo()
            }
        }
        val separateOffline = isReleaseDesign
        viewModel.lobby.observe(viewLifecycleOwner, Observer { result ->
            binding.lobbySwipeRefreshLayout.isRefreshing = false
            handle(result) { lobby ->
                val items = LobbyItem.parse(lobby, separateOffline)
                lobbyAdapter.submitList(items, lobby.tags, lobby.content, lobby.payloadId)
                binding.lobbyLoadingIndicator.fadeOutLoadingIndicator()
            }
        })
        if (isReleaseDesign) {
            viewModel.emailVerificationUser.observe(viewLifecycleOwner, Observer { user ->
                val email = user.email ?: return@Observer
                binding.verifyEmailContainer.isVisible = (user.emailVerified == false)
                binding.resendEmailButton.setOnClickListener {
                    viewModel.sendVerificationEmail()
                    fragmentManager?.navigateToSendingVerificationEmailDialog(email)
                }
            })
        }
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
}

class ClassicLobbyItemDecoration(
    private val resources: Resources,
    private val lobbyAdapter: GenericLobbyAdapter<*>
) : RecyclerView.ItemDecoration() {
    private val edgeOffset by lazy { resources.getDimensionPixelSize(R.dimen.lobby_card_side_margin) }
    private val listTopBottomOffset by lazy { resources.getDimensionPixelSize(R.dimen.lobby_list_top_bottom_margin) }
    private val cardSpacing by lazy { resources.getDimensionPixelSize(R.dimen.lobby_card_vertical_spacing) }
    private val headerTopMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_header_top_margin) }
    private val headerBottomMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_header_bottom_margin) }
    private val subtitleTopMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_subtitle_top_margin) }
    private val subtitleBottomMargin by lazy { resources.getDimensionPixelSize(R.dimen.lobby_subtitle_bottom_margin) }

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

class ReleaseLobbyItemDecoration(
    private val resources: Resources
) : RecyclerView.ItemDecoration() {
    private val cardMargin by lazy { resources.getDimensionPixelSize(R.dimen.release_lobby_card_vertical_spacing) }
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemType = view.tag as? LobbyItem.Type ?: return
        val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
        val cardTopMargin = if (itemPosition == 0) 0 else cardMargin
        when (itemType) {
            LobbyItem.Type.LIVE_BROADCAST_CARD,
            LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD,
            LobbyItem.Type.CARD_LIST,
            LobbyItem.Type.SINGLE_CATEGORY_CARD,
            LobbyItem.Type.CATEGORY_CARD_LIST -> outRect.set(0, cardTopMargin, 0, 0)
            LobbyItem.Type.DOUBLE_CATEGORY_CARD -> outRect.set(0, cardTopMargin / 2, 0, 0)
            else -> outRect.set(0, 0, 0, 0)
        }
    }
}
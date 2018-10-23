package tv.caffeine.app.lobby


import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.R
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentLobbyBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class LobbyFragment : CaffeineFragment() {

    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var lobbyAdapter: LobbyAdapter
    @Inject lateinit var tokenStore: TokenStore
    private val viewModel by lazy { viewModelProvider.get(LobbyViewModel::class.java) }
    private lateinit var binding: FragmentLobbyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (tokenStore.createRefreshTokenBody() == null) {
            findNavController().navigate(R.id.action_global_landingFragment)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentLobbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val edgeOffset = binding.root.resources.getDimension(R.dimen.lobby_card_side_margin).toInt()
        val listTopBottomOffset = binding.root.resources.getDimension(R.dimen.lobby_list_top_bottom_margin).toInt()
        val cardSpacing = binding.root.resources.getDimension(R.dimen.lobby_card_vertical_spacing).toInt()
        val headerTopMargin = binding.root.resources.getDimension(R.dimen.lobby_header_top_margin).toInt()
        val headerBottomMargin = binding.root.resources.getDimension(R.dimen.lobby_header_bottom_margin).toInt()
        val subtitleTopMargin = binding.root.resources.getDimension(R.dimen.lobby_subtitle_top_margin).toInt()
        val subtitleBottomMargin = binding.root.resources.getDimension(R.dimen.lobby_subtitle_bottom_margin).toInt()
        binding.lobbyRecyclerView.run {
            adapter = lobbyAdapter
            addItemDecoration(object: RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
                    val extraTopOffset = if (itemPosition == 0) listTopBottomOffset else 0
                    val extraBottomOffset = if (itemPosition == lobbyAdapter.itemCount - 1) listTopBottomOffset else 0
                    val viewType = lobbyAdapter.getItemViewType(itemPosition)
                    val itemType = LobbyItem.Type.values()[viewType]
                    when(itemType) {
                        LobbyItem.Type.LIVE_BROADCAST_CARD, LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD, LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> outRect.set(edgeOffset, extraTopOffset + cardSpacing, edgeOffset, extraBottomOffset + cardSpacing)
                        LobbyItem.Type.CARD_LIST -> outRect.set(0, extraTopOffset + cardSpacing, 0, extraBottomOffset + cardSpacing)
                        LobbyItem.Type.HEADER -> outRect.set(edgeOffset, extraTopOffset + headerTopMargin, edgeOffset, extraBottomOffset + headerBottomMargin)
                        LobbyItem.Type.SUBTITLE -> outRect.set(edgeOffset, extraTopOffset + subtitleTopMargin, edgeOffset, extraBottomOffset + subtitleBottomMargin)
                    }
                }
            })
            setRecycledViewPool(lobbyAdapter.recycledViewPool)
        }
        binding.profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.myProfileFragment))
        binding.searchButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.exploreFragment))
        binding.activityButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.notificationsFragment))
        binding.lobbySwipeRefreshLayout.setOnRefreshListener { refreshLobby() }
        binding.setLifecycleOwner(viewLifecycleOwner)
        viewModel.lobby.observe(viewLifecycleOwner, Observer {
            val items = LobbyItem.parse(it)
            lobbyAdapter.submitList(items, it.tags, it.content)
            binding.lobbySwipeRefreshLayout.isRefreshing = false
        })
    }

    private fun refreshLobby() {
        followManager.refreshFollowedUsers()
        viewModel.refresh()
    }

}

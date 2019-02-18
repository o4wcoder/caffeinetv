package tv.caffeine.app.lobby


import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.broadcast.BroadcastPlaceholderDialogFragment
import tv.caffeine.app.databinding.FragmentLobbyBinding
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.safeNavigate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LobbyFragment : CaffeineFragment() {

    @Inject lateinit var lobbyAdapter: LobbyAdapter
    @Inject lateinit var featureConfig: FeatureConfig
    @Inject lateinit var picasso: Picasso

    private val viewModel by lazy { viewModelProvider.get(LobbyViewModel::class.java) }
    private val myProfileViewModel by lazy { viewModelProvider.get(MyProfileViewModel::class.java) }
    private var binding: FragmentLobbyBinding? = null
    private var refreshJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val binding = FragmentLobbyBinding.inflate(inflater, container, false)
        configure(binding)
        this.binding = binding
        return binding.root
    }

    private fun configure(binding: FragmentLobbyBinding) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.lobbyRecyclerView.run {
            adapter = lobbyAdapter
            addItemDecoration(itemDecorator)
        }
        binding.cameraButton.setOnClickListener {
            if (featureConfig.isFeatureEnabled(Feature.BROADCAST)) {
                val action = LobbyFragmentDirections.actionLobbyFragmentToBroadcastFragment()
                findNavController().safeNavigate(action)
            } else {
                BroadcastPlaceholderDialogFragment().show(fragmentManager, "broadcastPlaceholder")
            }
        }
        binding.profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.myProfileFragment))
        binding.searchButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.exploreFragment))
        binding.activityButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.notificationsFragment))
        binding.lobbySwipeRefreshLayout.setOnRefreshListener { refreshLobby() }
        viewModel.lobby.observe(viewLifecycleOwner, Observer { result ->
            binding.lobbySwipeRefreshLayout.isRefreshing = false
            handle(result) { lobby ->
                val items = LobbyItem.parse(lobby)
                lobbyAdapter.submitList(items, lobby.tags, lobby.content)
                binding.lobbyLoadingIndicator.isVisible = false
            }
        })
        myProfileViewModel.avatarImageUrl.observe(viewLifecycleOwner, Observer {  avatarImageUrl ->
            picasso
                    .load(avatarImageUrl)
                    .resizeDimen(R.dimen.toolbar_icon_size, R.dimen.toolbar_icon_size)
                    .transform(CropCircleTransformation())
                    .into(binding.profileButton)
        })
        myProfileViewModel.emailVerified.observe(viewLifecycleOwner, Observer { emailVerified ->
            binding.unverifiedMessageTextView.isVisible = emailVerified == false
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
            while(isActive) {
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

    private val edgeOffset by lazy { resources.getDimension(R.dimen.lobby_card_side_margin).toInt() }
    private val listTopBottomOffset by lazy { resources.getDimension(R.dimen.lobby_list_top_bottom_margin).toInt() }
    private val cardSpacing by lazy { resources.getDimension(R.dimen.lobby_card_vertical_spacing).toInt() }
    private val headerTopMargin by lazy { resources.getDimension(R.dimen.lobby_header_top_margin).toInt() }
    private val headerBottomMargin by lazy { resources.getDimension(R.dimen.lobby_header_bottom_margin).toInt() }
    private val subtitleTopMargin by lazy { resources.getDimension(R.dimen.lobby_subtitle_top_margin).toInt() }
    private val subtitleBottomMargin by lazy { resources.getDimension(R.dimen.lobby_subtitle_bottom_margin).toInt() }

    private val itemDecorator = object: RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val itemType = view.tag as? LobbyItem.Type ?: return
            val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
            val extraTopOffset = if (itemPosition == 0) listTopBottomOffset else 0
            val extraBottomOffset = if (itemPosition == lobbyAdapter.itemCount - 1) listTopBottomOffset else 0
            when(itemType) {
                LobbyItem.Type.AVATAR_CARD -> outRect.set(0, extraTopOffset + cardSpacing, 0, extraBottomOffset + cardSpacing)
                LobbyItem.Type.LIVE_BROADCAST_CARD, LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD, LobbyItem.Type.PREVIOUS_BROADCAST_CARD -> outRect.set(edgeOffset, extraTopOffset + cardSpacing, edgeOffset, extraBottomOffset + cardSpacing)
                LobbyItem.Type.CARD_LIST -> outRect.set(0, extraTopOffset + cardSpacing, 0, extraBottomOffset + cardSpacing)
                LobbyItem.Type.HEADER -> outRect.set(edgeOffset, extraTopOffset + headerTopMargin, edgeOffset, extraBottomOffset + headerBottomMargin)
                LobbyItem.Type.SUBTITLE -> outRect.set(edgeOffset, extraTopOffset + subtitleTopMargin, edgeOffset, extraBottomOffset + subtitleBottomMargin)
            }
        }
    }
}

package tv.caffeine.app.lobby


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
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
            findNavController().navigate(R.id.action_lobbyFragment_to_landingFragment)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentLobbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lobbyRecyclerView.run {
            adapter = lobbyAdapter
            setRecycledViewPool(lobbyAdapter.recycledViewPool)
        }
        binding.profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.myProfileFragment))
        binding.searchButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.exploreFragment))
        binding.activityButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.notificationsFragment))
        binding.lobbySwipeRefreshLayout.setOnRefreshListener { refreshLobby() }
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

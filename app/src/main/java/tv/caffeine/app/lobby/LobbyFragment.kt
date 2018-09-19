package tv.caffeine.app.lobby


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import tv.caffeine.app.R
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentLobbyBinding
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class LobbyFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var followManager: FollowManager
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    @Inject lateinit var lobbyAdapter: LobbyAdapter
    @Inject lateinit var tokenStore: TokenStore
    private var viewModel: LobbyViewModel? = null
    private var binding: FragmentLobbyBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (tokenStore.createRefreshTokenBody() == null) {
            findNavController().navigate(R.id.action_lobbyFragment_to_landingFragment)
        }
        viewModel = viewModelProvider.get(LobbyViewModel::class.java).apply {
            lobby.observe(this@LobbyFragment, Observer {
                val items = LobbyItem.parse(it)
                lobbyAdapter.submitList(items, it.tags, it.content)
                binding?.lobbySwipeRefreshLayout?.isRefreshing = false
            })
        }
        refreshLobby()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val binding = FragmentLobbyBinding.inflate(inflater, container, false).apply {
            lobbyRecyclerView.run {
                adapter = lobbyAdapter
                setRecycledViewPool(lobbyAdapter.recycledViewPool)
            }
            profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.myProfileFragment))
            searchButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.exploreFragment))
            activityButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.notificationsFragment))
            lobbySwipeRefreshLayout.setOnRefreshListener { refreshLobby() }
        }
        this.binding = binding
        return binding.root
    }

    private fun refreshLobby() {
        followManager.refreshFollowedUsers()
        viewModel?.refresh()
    }

}

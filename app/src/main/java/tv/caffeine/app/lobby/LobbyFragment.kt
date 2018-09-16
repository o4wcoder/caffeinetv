package tv.caffeine.app.lobby


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_lobby.*
import tv.caffeine.app.R
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class LobbyFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var followManager: FollowManager
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    @Inject lateinit var lobbyAdapter: LobbyAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lobby_recycler_view.layoutManager = LinearLayoutManager(context)
        lobby_recycler_view.adapter = lobbyAdapter
        lobby_recycler_view.setRecycledViewPool(lobbyAdapter.recycledViewPool)
        profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.myProfileFragment))
        searchButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.exploreFragment))
        activityButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.notificationsFragment))
        loadLobby()
    }

    private fun loadLobby() {
        val viewModel = viewModelProvider.get(LobbyViewModel::class.java)
        viewModel.lobby.observe(this, Observer {
            val items = LobbyItem.parse(it)
            lobbyAdapter.submitList(items, it.tags, it.content)
            lobby_swipe_refresh_layout.isRefreshing = false
        })
        lobby_swipe_refresh_layout.setOnRefreshListener { refreshLobby(viewModel) }
        refreshLobby(viewModel)
    }

    private fun refreshLobby(viewModel: LobbyViewModel) {
        followManager.refreshFollowedUsers()
        viewModel.refresh()
    }

}

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lobby_recycler_view.layoutManager = LinearLayoutManager(context)
        lobby_recycler_view.adapter = LobbyAdapter(listOf(), mapOf(), mapOf(), followManager, lobby_recycler_view.recycledViewPool)
        profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.profile))
        searchButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.exploreFragment))
        loadLobby()
    }

    private fun loadLobby() {
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(LobbyViewModel::class.java)
        viewModel.lobby.observe(this, Observer {
            val items = LobbyItem.parse(it)
            lobby_recycler_view.adapter = LobbyAdapter(items, it.tags, it.content, followManager, lobby_recycler_view.recycledViewPool)
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

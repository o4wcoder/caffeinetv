package tv.caffeine.app.lobby


import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_lobby.*
import tv.caffeine.app.R
import tv.caffeine.app.di.LobbyViewModelFactory
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class LobbyFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: LobbyViewModelFactory
    @Inject lateinit var followManager: FollowManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        followManager.refreshFollowedUsers()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(lobby_toolbar)
        lobby_recycler_view.layoutManager = LinearLayoutManager(context)
        lobby_recycler_view.adapter = LobbyAdapter(listOf(), mapOf(), mapOf(), followManager)
        loadLobby()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.lobby, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.profile -> {
                findNavController().navigate(R.id.profile)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadLobby() {
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(LobbyViewModel::class.java)
        viewModel.getLobby().observe(this, Observer {
            val items = LobbyItem.parse(it)
            lobby_recycler_view.adapter = LobbyAdapter(items, it.tags, it.content, followManager)
        })
    }

}

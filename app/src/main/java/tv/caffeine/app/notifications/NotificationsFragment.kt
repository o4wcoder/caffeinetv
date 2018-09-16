package tv.caffeine.app.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerFragment
import tv.caffeine.app.databinding.NotificationsFragmentBinding
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class NotificationsFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var followManager: FollowManager
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    private lateinit var viewModel: NotificationsViewModel
    @Inject lateinit var notificationsAdapter: NotificationsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = NotificationsFragmentBinding.inflate(layoutInflater, container, false)
        binding.notificationsRecyclerView.adapter = notificationsAdapter
        viewModel = viewModelProvider.get(NotificationsViewModel::class.java)
        viewModel.followers.observe(this, Observer {
            notificationsAdapter.submitList(it)
        })
        viewModel.refresh()
        return binding.root
    }

}

package tv.caffeine.app.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.users.CaidListAdapter
import javax.inject.Inject

class NotificationsFragment : CaffeineFragment() {

    private lateinit var viewModel: NotificationsViewModel
    @Inject lateinit var caidListAdapter: CaidListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        binding.userListRecyclerView.adapter = caidListAdapter
        viewModel = viewModelProvider.get(NotificationsViewModel::class.java)
        viewModel.followers.observe(this, Observer {
            caidListAdapter.submitList(it)
        })
        return binding.root
    }

}

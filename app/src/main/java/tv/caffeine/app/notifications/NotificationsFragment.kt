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

    @Inject lateinit var caidListAdapter: CaidListAdapter

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var binding: UserListFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.userListRecyclerView.adapter = caidListAdapter
        viewModel = viewModelProvider.get(NotificationsViewModel::class.java)
        viewModel.followers.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
    }
}

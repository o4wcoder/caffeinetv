package tv.caffeine.app.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class NotificationsFragment : CaffeineFragment() {

    @Inject lateinit var notificationsAdapter: NotificationsAdapter

    private val viewModel by lazy { viewModelProvider.get(NotificationsViewModel::class.java) }
    private lateinit var binding: UserListFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.userListRecyclerView.adapter = notificationsAdapter
        viewModel.notifications.observe(viewLifecycleOwner, Observer {
            notificationsAdapter.submitList(it)
            viewModel.markNotificationsViewed()
        })
    }
}

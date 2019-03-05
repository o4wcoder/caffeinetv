package tv.caffeine.app.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.setItemDecoration
import javax.inject.Inject

class NotificationsFragment : CaffeineFragment() {

    @Inject lateinit var notificationsAdapter: NotificationsAdapter

    private val viewModel: NotificationsViewModel by viewModels { viewModelFactory }
    private lateinit var binding: UserListFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        notificationsAdapter.fragmentManager = fragmentManager
        binding.userListRecyclerView.apply {
            adapter = notificationsAdapter
            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        viewModel.notifications.observe(viewLifecycleOwner, Observer {
            notificationsAdapter.submitList(it)
            viewModel.markNotificationsViewed()
        })
    }
}

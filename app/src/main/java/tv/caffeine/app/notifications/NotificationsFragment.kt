package tv.caffeine.app.notifications

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import tv.caffeine.app.R
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.fadeOutLoadingIndicator
import tv.caffeine.app.util.setItemDecoration
import javax.inject.Inject

class NotificationsFragment @Inject constructor(
    private val notificationsAdapter: NotificationsAdapter
) : CaffeineFragment(R.layout.user_list_fragment) {

    private val viewModel: NotificationsViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = UserListFragmentBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        notificationsAdapter.fragmentManager = fragmentManager
        binding.loadingIndicator.isVisible = true
        binding.userListRecyclerView.apply {
            adapter = notificationsAdapter
            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        viewModel.notifications.observe(viewLifecycleOwner, Observer {
            binding.loadingIndicator.fadeOutLoadingIndicator()
            notificationsAdapter.submitList(it)
            viewModel.markNotificationsViewed()
        })

        observeFollowEvents()
    }
}

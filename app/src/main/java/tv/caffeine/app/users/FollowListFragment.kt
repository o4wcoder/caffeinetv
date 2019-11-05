package tv.caffeine.app.users

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.ThemeColor
import tv.caffeine.app.util.fadeOutLoadingIndicator
import tv.caffeine.app.util.setItemDecoration

abstract class FollowListFragment(private val userListAdapter: UserListAdapter) :
    CaffeineFragment(R.layout.user_list_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = UserListFragmentBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        val viewModel = getFollowListViewModel()
        binding.viewModel = viewModel
        viewModel.isDarkMode = isDarkMode()
        binding.loadingIndicator.isVisible = true
        userListAdapter.usernameThemeColor = if (isDarkMode()) ThemeColor.DARK else ThemeColor.LIGHT
        binding.userListRecyclerView.apply {
            adapter = userListAdapter
            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        observeFollowEvents()

        viewModel.init(getCAID())
        viewModel.liveData?.observe(viewLifecycleOwner, Observer {
            userListAdapter.submitList(it)
        })
        viewModel.isRefreshingState.observe(viewLifecycleOwner, Observer { isRefreshing ->
            when {
                isRefreshing -> binding.loadingIndicator.isVisible = true
                else -> binding.loadingIndicator.fadeOutLoadingIndicator()
            }
        })
        viewModel.isEmptyState.observe(viewLifecycleOwner, Observer { isEmpty ->
            binding.userListEmptyTextView.text = if (isEmpty) getString(getEmptyMessageResId(), getUsername()) else null
        })
    }

    abstract fun getFollowListViewModel(): FollowListViewModel

    abstract fun getCAID(): CAID

    abstract fun getUsername(): String

    abstract fun isDarkMode(): Boolean

    abstract fun getEmptyMessageResId(): Int
}

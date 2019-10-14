package tv.caffeine.app.users

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.ThemeColor
import tv.caffeine.app.util.setItemDecoration

abstract class FollowListFragment(private val caidListAdapter: CaidListAdapter) :
    CaffeineFragment(R.layout.user_list_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = UserListFragmentBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        val viewModel = getFollowListViewModel()
        binding.viewModel = viewModel
        viewModel.isDarkMode = isDarkMode()
        caidListAdapter.fragmentManager = fragmentManager
        val usernameThemeColor =
            if (isDarkMode()) ThemeColor.DARK else ThemeColor.LIGHT
        caidListAdapter.setUsernameFollowStarColor(usernameThemeColor)
        binding.userListRecyclerView.apply {
            adapter = caidListAdapter
            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        observeFollowEvents()

        viewModel.caid = getCAID()
        viewModel.followList.observe(viewLifecycleOwner, Observer {
            viewModel.isEmptyFollowList = it.isEmpty()
            if (it.isEmpty()) {
                viewModel.loadUserProfile(getCAID())
                    .observe(viewLifecycleOwner, Observer {
                        binding.userListEmptyTextView.text =
                            getString(getEmptyMessageResId(), it.username)
                    })
            } else {
                caidListAdapter.submitList(it)
            }
        })
    }

    abstract fun getFollowListViewModel(): FollowListViewModel

    abstract fun getCAID(): CAID

    abstract fun isDarkMode(): Boolean

    abstract fun getEmptyMessageResId(): Int
}
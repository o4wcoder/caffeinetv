package tv.caffeine.app.users

import android.content.Context
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import javax.inject.Inject

class IgnoredUsersFragment @Inject constructor(
    userListAdapter: UserListAdapter
) : FollowListFragment(userListAdapter) {

    init {
        userListAdapter.allowFollowing = false
    }

    private val viewModel: IgnoredUsersViewModel by viewModels { viewModelFactory }
    private val args by navArgs<IgnoredUsersFragmentArgs>()

    override fun getFollowListViewModel() = viewModel

    override fun getCAID() = args.caid
    override fun getUsername() = args.username

    override fun isDarkMode() = false

    override fun getEmptyMessageResId() = R.string.empty_ignored_users_list
}

class IgnoredUsersViewModel @Inject constructor(
    context: Context,
    pagedIgnoredUsersService: PagedIgnoredUsersService
) : FollowListViewModel(context, pagedIgnoredUsersService)

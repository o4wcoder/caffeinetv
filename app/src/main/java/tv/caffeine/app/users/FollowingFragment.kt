package tv.caffeine.app.users

import android.content.Context
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import javax.inject.Inject

class FollowingFragment @Inject constructor(
    userListAdapter: UserListAdapter
) : FollowListFragment(userListAdapter) {

    private val viewModel: FollowingViewModel by viewModels { viewModelFactory }
    private val args by navArgs<FollowingFragmentArgs>()

    override fun getFollowListViewModel() = viewModel

    override fun getCAID() = args.caid
    override fun getUsername() = args.username

    override fun isDarkMode() = args.isDarkMode

    override fun getEmptyMessageResId() = R.string.stage_profile_empty_following
}

class FollowingViewModel @Inject constructor(
    context: Context,
    pagedFollowedUsersService: PagedFollowedUsersService
) : FollowListViewModel(context, pagedFollowedUsersService)

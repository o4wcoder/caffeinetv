package tv.caffeine.app.users

import android.content.Context
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import javax.inject.Inject

class FollowersFragment @Inject constructor(
    userListAdapter: UserListAdapter
) : FollowListFragment(userListAdapter) {

    private val viewModel: FollowersViewModel by viewModels { viewModelFactory }
    private val args by navArgs<FollowersFragmentArgs>()

    override fun getFollowListViewModel() = viewModel

    override fun getCAID() = args.caid
    override fun getUsername() = args.username

    override fun isDarkMode() = args.isDarkMode

    override fun getEmptyMessageResId() = R.string.stage_profile_empty_followers
}

class FollowersViewModel @Inject constructor(
    context: Context,
    pagedFollowersService: PagedFollowersService
) : FollowListViewModel(context, pagedFollowersService)


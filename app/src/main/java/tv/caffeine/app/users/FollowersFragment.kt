package tv.caffeine.app.users

import android.content.Context
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class FollowersFragment @Inject constructor(
    caidListAdapter: CaidListAdapter
) : FollowListFragment(caidListAdapter) {

    private val viewModel: FollowersViewModel by viewModels { viewModelFactory }
    private val args by navArgs<FollowersFragmentArgs>()

    override fun getFollowListViewModel() = viewModel

    override fun getCAID() = args.caid

    override fun isDarkMode() = args.isDarkMode

    override fun getEmptyMessageResId() = R.string.stage_profile_empty_followers
}

class FollowersViewModel @Inject constructor(
    context: Context,
    val gson: Gson,
    val usersService: UsersService,
    val followManager: FollowManager,
    val profileRepository: ProfileRepository
) : FollowListViewModel(context, profileRepository) {

    override fun loadFollowList() {
        viewModelScope.launch {
            try {
                val result = usersService.listFollowers(caid)
                val userIDs = result.followers.subList(0, 20).map { it.caid }.distinct()
                val followerDetails = followManager.loadMultipleUserDetails(userIDs)
                setFollowList(result.followers)
            } catch(e: Exception) {
                Timber.e(e)
            }
        }
    }
}

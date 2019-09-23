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
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.repository.ProfileRepository
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
    val profileRepository: ProfileRepository
) : FollowListViewModel(context, profileRepository) {

    override fun loadFollowList() {
        viewModelScope.launch {
            val result = usersService.listFollowers(caid).awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> setFollowList(result.value.followers)
                is CaffeineResult.Error -> Timber.e("Error loading followers list ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }
}

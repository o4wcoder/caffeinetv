package tv.caffeine.app.users

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.FollowStarColor
import javax.inject.Inject

class IgnoredUsersFragment @Inject constructor(
    private val caidListAdapter: CaidListAdapter
) : CaffeineFragment(R.layout.user_list_fragment) {

    private val viewModel: IgnoredUsersViewModel by viewModels { viewModelFactory }
    private lateinit var binding: UserListFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = UserListFragmentBinding.bind(view)
        caidListAdapter.setUsernameFollowStarColor(FollowStarColor.BLACK)
        binding.userListRecyclerView.adapter = caidListAdapter
        viewModel.ignoredUsers.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
    }
}

class IgnoredUsersViewModel @Inject constructor(
    private val gson: Gson,
    private val tokenStore: TokenStore,
    private val usersService: UsersService
) : ViewModel() {

    private val _ignoredUsers = MutableLiveData<List<CaidRecord.IgnoreRecord>>()

    val ignoredUsers: LiveData<List<CaidRecord.IgnoreRecord>> = _ignoredUsers.map { it }

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        viewModelScope.launch {
            val result = usersService.listIgnoredUsers(caid).awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> _ignoredUsers.value = result.value
                is CaffeineResult.Error -> Timber.e("Failed to load ignored users ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }
}

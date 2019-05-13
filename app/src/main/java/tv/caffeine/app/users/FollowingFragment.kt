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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.setItemDecoration
import javax.inject.Inject

class FollowingFragment @Inject constructor(
    private val caidListAdapter: CaidListAdapter
) : CaffeineFragment(R.layout.user_list_fragment) {

    private val viewModel: FollowingViewModel by viewModels { viewModelFactory }
    private val args by navArgs<FollowingFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = UserListFragmentBinding.bind(view)
        caidListAdapter.fragmentManager = fragmentManager
        binding.userListRecyclerView.apply {
            adapter = caidListAdapter
            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        viewModel.caid = args.caid
        viewModel.following.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
    }
}

class FollowingViewModel @Inject constructor(
    private val gson: Gson,
    private val usersService: UsersService
) : ViewModel() {

    private val _following = MutableLiveData<List<CaidRecord.FollowRecord>>()

    val following: LiveData<List<CaidRecord.FollowRecord>> = _following.map { it }

    var caid: CAID = ""
        set(value) {
            field = value
            load()
        }

    private fun load() {
        viewModelScope.launch {
            val result = usersService.listFollowing(caid).awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> _following.value = result.value
                is CaffeineResult.Error -> Timber.e("Error loading following list ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }
}

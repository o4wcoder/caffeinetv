package tv.caffeine.app.users

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
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
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.setItemDecoration
import javax.inject.Inject

class FollowersFragment : CaffeineFragment(R.layout.user_list_fragment) {

    @Inject lateinit var caidListAdapter: CaidListAdapter

    private val viewModel: FollowersViewModel by viewModels { viewModelFactory }
    private val args by navArgs<FollowersFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = UserListFragmentBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        caidListAdapter.fragmentManager = fragmentManager
        binding.userListRecyclerView.apply {
            adapter = caidListAdapter
            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        viewModel.caid = args.caid
        viewModel.followers.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
    }
}

class FollowersViewModel @Inject constructor(
        dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val usersService: UsersService
) : CaffeineViewModel(dispatchConfig) {

    private val _followers = MutableLiveData<List<CaidRecord.FollowRecord>>()
    val followers: LiveData<List<CaidRecord.FollowRecord>> = Transformations.map(_followers) { it }

    var caid: CAID = ""
        set(value) {
            field = value
            load()
        }

    private fun load() {
        launch {
            val result = usersService.listFollowers(caid).awaitAndParseErrors(gson)
            when(result) {
                is CaffeineResult.Success -> _followers.value = result.value
                is CaffeineResult.Error -> Timber.e("Error loading followers list ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

}

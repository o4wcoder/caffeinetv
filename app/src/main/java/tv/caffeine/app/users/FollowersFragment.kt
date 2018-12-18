package tv.caffeine.app.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.setItemDecoration
import javax.inject.Inject

class FollowersFragment : CaffeineFragment() {

    @Inject lateinit var caidListAdapter: CaidListAdapter

    private val viewModel by lazy { viewModelProvider.get(FollowersViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        caidListAdapter.fragmentManager = fragmentManager
        binding.userListRecyclerView.apply {
            adapter = caidListAdapter
            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        viewModel.caid = FollowersFragmentArgs.fromBundle(arguments).caid
        viewModel.followers.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
        return binding.root
    }
}

class FollowersViewModel(
        dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val usersService: UsersService
) : CaffeineViewModel(dispatchConfig) {

    private val _followers = MutableLiveData<List<CaidRecord.FollowRecord>>()
    val followers: LiveData<List<CaidRecord.FollowRecord>> = Transformations.map(_followers) { it }

    var caid: String = ""
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

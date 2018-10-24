package tv.caffeine.app.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class FollowingFragment : CaffeineFragment() {

    private val viewModel by lazy { viewModelProvider.get(FollowingViewModel::class.java) }
    @Inject lateinit var caidListAdapter: CaidListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        binding.userListRecyclerView.adapter = caidListAdapter
        viewModel.caid = FollowingFragmentArgs.fromBundle(arguments).caid
        viewModel.following.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
        return binding.root
    }

}

class FollowingViewModel(private val usersService: UsersService) : CaffeineViewModel() {

    val following: LiveData<List<CaidRecord.FollowRecord>> get() = _following

    private val _following = MutableLiveData<List<CaidRecord.FollowRecord>>()

    var caid: String = ""
        set(value) {
            field = value
            load()
        }

    private fun load() {
        launch {
            val list = usersService.listFollowing(caid).await()
            withContext(Dispatchers.Main) {
                _following.value = list
            }
        }
    }

}


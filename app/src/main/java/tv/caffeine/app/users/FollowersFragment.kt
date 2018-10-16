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
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class FollowersFragment : CaffeineFragment() {

    private val viewModel by lazy { viewModelProvider.get(FollowersViewModel::class.java) }
    @Inject lateinit var caidListAdapter: CaidListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        binding.userListRecyclerView.adapter = caidListAdapter
        viewModel.followers.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
        return binding.root
    }

}

class FollowersViewModel(private val tokenStore: TokenStore, private val usersService: UsersService) : CaffeineViewModel() {

    val followers: LiveData<List<CaidRecord.FollowRecord>> get() = _followers

    private val _followers = MutableLiveData<List<CaidRecord.FollowRecord>>()

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        launch {
            val list = usersService.listFollowers(caid).await()
            withContext(Dispatchers.Main) {
                _followers.value = list
            }
        }
    }

}


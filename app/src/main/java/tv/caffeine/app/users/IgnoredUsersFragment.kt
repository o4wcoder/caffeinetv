package tv.caffeine.app.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import kotlinx.coroutines.launch
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.UserListFragmentBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class IgnoredUsersFragment : CaffeineFragment() {

    @Inject lateinit var caidListAdapter: CaidListAdapter

    private val viewModel by lazy { viewModelProvider.get(IgnoredUsersViewModel::class.java) }
    private lateinit var binding: UserListFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = UserListFragmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.userListRecyclerView.adapter = caidListAdapter
        viewModel.ignoredUsers.observe(viewLifecycleOwner, Observer {
            caidListAdapter.submitList(it)
        })
    }
}

class IgnoredUsersViewModel(
        dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val usersService: UsersService
) : CaffeineViewModel(dispatchConfig) {

    private val _ignoredUsers = MutableLiveData<List<CaidRecord.IgnoreRecord>>()

    val ignoredUsers: LiveData<List<CaidRecord.IgnoreRecord>> = Transformations.map(_ignoredUsers) { it }

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        launch {
            val list = usersService.listIgnoredUsers(caid).await()
            _ignoredUsers.value = list
        }
    }

}

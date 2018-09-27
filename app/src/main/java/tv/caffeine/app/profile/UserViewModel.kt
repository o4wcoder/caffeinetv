package tv.caffeine.app.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.session.FollowManager

class UserViewModel(
        private val followManager: FollowManager,
        private val usersService: UsersService
) : ViewModel() {
    val username = MutableLiveData<String>()

    val job = GlobalScope.launch {
        val user = followManager.userDetails("alpha")
    }
}
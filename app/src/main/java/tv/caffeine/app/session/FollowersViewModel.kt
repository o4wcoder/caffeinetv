package tv.caffeine.app.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.api.UsersService

class FollowersViewModel(private val caid: String, private val usersService: UsersService) : ViewModel() {
    private val followers: MutableLiveData<List<String>> = MutableLiveData()
    private var list: List<String>? = null
    private var job: Job? = null

    fun getFollowers(): LiveData<List<String>> {
        if (list == null) loadFollowers()
        return followers
    }

    private fun loadFollowers() {
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.Default) {
            val result = usersService.listFollowers(caid).await()
            launch(Dispatchers.Main) {
                followers.value = result.map { it.caid }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
package tv.caffeine.app.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.ui.CaffeineViewModel

class FollowersViewModel(private val caid: String, private val usersService: UsersService) : CaffeineViewModel() {
    private val _followers: MutableLiveData<List<String>> = MutableLiveData()
    val followers: LiveData<List<String>> get() = _followers

    init {
        load()
    }

    private fun load() {
        launch {
            val result = usersService.listFollowers(caid).await()
            withContext(Dispatchers.Main) {
                _followers.value = result.map { it.caid }
            }
        }
    }
}
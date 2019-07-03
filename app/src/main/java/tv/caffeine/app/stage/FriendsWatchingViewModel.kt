package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class FriendsWatchingViewModel @Inject constructor(
    private val followManager: FollowManager,
    private var friendsWatchingController: FriendsWatchingController
) : ViewModel() {
    private val friendsWatchingSet: MutableSet<CAID> = mutableSetOf()

    private val _friendsWatching = MutableLiveData<List<User>>()
    val friendsWatching: LiveData<List<User>> = _friendsWatching.map { it }

    fun load(stageIdentifier: String) {
        viewModelScope.launch {
            friendsWatchingController.connect(stageIdentifier).collect { event ->
                if (event.isViewing) {
                    friendsWatchingSet.add(event.caid)
                } else {
                    friendsWatchingSet.remove(event.caid)
                }
                _friendsWatching.value = friendsWatchingSet.mapNotNull { caid ->
                    followManager.userDetails(caid)
                }
            }
        }
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }

    fun disconnect() {
        viewModelScope.coroutineContext.cancelChildren()
    }
}

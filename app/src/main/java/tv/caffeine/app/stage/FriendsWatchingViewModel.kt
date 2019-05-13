package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class FriendsWatchingViewModel @Inject constructor(
    private val followManager: FollowManager,
    private val friendsWatchingControllerFactory: FriendsWatchingController.Factory
) : ViewModel() {
    private var friendsWatchingController: FriendsWatchingController? = null
    private val friendsWatchingSet: MutableSet<CAID> = mutableSetOf()

    private val _friendsWatching = MutableLiveData<List<User>>()
    val friendsWatching: LiveData<List<User>> = Transformations.map(_friendsWatching) { it }

    fun load(stageIdentifier: String) {
        friendsWatchingController = friendsWatchingControllerFactory.create(stageIdentifier)
        viewModelScope.launch {
            friendsWatchingController?.channel?.consumeEach { event ->
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
        friendsWatchingController?.close()
    }
}

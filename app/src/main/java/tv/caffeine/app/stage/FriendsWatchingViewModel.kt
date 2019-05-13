package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class FriendsWatchingViewModel @Inject constructor(
    dispatchConfig: DispatchConfig,
    private val followManager: FollowManager,
    private val friendsWatchingControllerFactory: FriendsWatchingController.Factory
) : CaffeineViewModel(dispatchConfig) {
    private var friendsWatchingController: FriendsWatchingController? = null
    private val friendsWatchingSet: MutableSet<CAID> = mutableSetOf()

    private val _friendsWatching = MutableLiveData<List<User>>()
    val friendsWatching: LiveData<List<User>> = Transformations.map(_friendsWatching) { it }

    fun load(stageIdentifier: String) {
        friendsWatchingController = friendsWatchingControllerFactory.create(stageIdentifier)
        launch {
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

package tv.caffeine.app.stage


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.databinding.FragmentFriendsWatchingBinding
import tv.caffeine.app.notifications.NotificationsAdapter
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import javax.inject.Inject

class FriendsWatchingFragment : CaffeineBottomSheetDialogFragment() {
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var broadcastsService: BroadcastsService
    @Inject lateinit var usersAdapter: NotificationsAdapter

    private lateinit var broadcaster: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = FriendsWatchingFragmentArgs.fromBundle(arguments)
        broadcaster = args.broadcaster
        launch(Dispatchers.Default) {
            val userDetails = followManager.userDetails(broadcaster)
            val friendsWatching = broadcastsService.friendsWatching(userDetails.broadcastId)
            withContext(Dispatchers.Main) {
                usersAdapter.submitList(friendsWatching.await())
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentFriendsWatchingBinding.inflate(inflater, container, false)
        binding.usersRecyclerView.adapter = usersAdapter
        return binding.root
    }

}

package tv.caffeine.app.stage


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentFriendsWatchingBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.users.CaidListAdapter
import javax.inject.Inject

class FriendsWatchingFragment : CaffeineBottomSheetDialogFragment() {
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var broadcastsService: BroadcastsService
    @Inject lateinit var usersAdapter: CaidListAdapter
    @Inject lateinit var gson: Gson

    private lateinit var broadcaster: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = FriendsWatchingFragmentArgs.fromBundle(arguments)
        broadcaster = args.broadcaster
        launch {
            val userDetails = followManager.userDetails(broadcaster) ?: return@launch
            val broadcastId = userDetails.broadcastId ?: return@launch
            val result = broadcastsService.friendsWatching(broadcastId).awaitAndParseErrors(gson)
            when(result) {
                is CaffeineResult.Success -> usersAdapter.submitList(result.value)
                is CaffeineResult.Error -> Timber.e(Exception("Failed to fetch friends watching ${result.error}"))
                is CaffeineResult.Failure -> Timber.e(result.throwable)
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

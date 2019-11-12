package tv.caffeine.app.stage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.databinding.FragmentFriendsWatchingBinding
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.users.CaidListAdapter
import tv.caffeine.app.users.UserNavigationCallback
import tv.caffeine.app.util.ThemeColor
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

class FriendsWatchingFragment @Inject constructor(
    private val userCaidAdapter: CaidListAdapter,
    val releaseDesignConfig: ReleaseDesignConfig
) : CaffeineBottomSheetDialogFragment() {

    private val args by navArgs<FriendsWatchingFragmentArgs>()
    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentFriendsWatchingBinding.inflate(inflater, container, false)
        binding.isReleaseActive = releaseDesignConfig.isReleaseDesignActive()
        val userNavigationCallback = object : UserNavigationCallback {
            override fun onUserNavigation(action: NavDirections) {
                findNavController().safeNavigate(action)
            }
        }
        userCaidAdapter.setUserNavigationCallback(userNavigationCallback)
        userCaidAdapter.setUsernameFollowStarColor(ThemeColor.DARK)
        binding.usersRecyclerView.adapter = userCaidAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stageIdentifier = args.stageIdentifier
        friendsWatchingViewModel.load(stageIdentifier)
        friendsWatchingViewModel.friendsWatching.observe(viewLifecycleOwner, Observer {
            userCaidAdapter.submitList(it.map { CaidRecord.FriendWatching(it.caid) })
        })
    }
}

package tv.caffeine.app.profile


import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentProfileBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.safeNavigate

class ProfileFragment : CaffeineFragment() {

    private val viewModel by lazy { viewModelProvider.get(ProfileViewModel::class.java) }
    private lateinit var caid: String
    private lateinit var binding: FragmentProfileBinding
    private var username: String? = null
    private var isFollowed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        caid = ProfileFragmentArgs.fromBundle(arguments).caid
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel.load(caid)
        viewModel.username.observe(this, Observer { username ->
            this.username = username
            binding.moreButton.apply {
                visibility = View.VISIBLE
                setOnClickListener { fragmentManager?.navigateToReportOrIgnoreDialog(caid, username, true) }
            }
        })
        viewModel.isFollowed.observe(this, Observer { isFollowed ->
            this.isFollowed = isFollowed
            binding.followButton.apply {
                visibility = View.VISIBLE
                setText(if (isFollowed) R.string.following_button else R.string.follow_button)
            }
        })
        binding.followButton.setOnClickListener { if (isFollowed) promptToUnfollow() else viewModel.follow(caid) }
        binding.profileViewModel = viewModel
        binding.setLifecycleOwner(viewLifecycleOwner)
        binding.followingContainer.setOnClickListener { showFollowingList() }
        binding.followerContainer.setOnClickListener { showFollowersList() }
        binding.stageImageView.setOnClickListener { watchBroadcast() }
        return binding.root
    }

    private fun promptToUnfollow() {
        val action = ProfileFragmentDirections.actionProfileFragmentToUnfollowUserDialogFragment(username ?: "user")
        val fragment = UnfollowUserDialogFragment()
        fragment.positiveClickListener = DialogInterface.OnClickListener { _, _ -> viewModel.unfollow(caid) }
        fragment.arguments = action.arguments
        fragment.show(fragmentManager, "unfollowUser")
    }

    private fun showFollowingList() {
        val action = ProfileFragmentDirections.actionProfileFragmentToFollowingFragment(caid)
        findNavController().safeNavigate(action)
    }

    private fun showFollowersList() {
        val action = ProfileFragmentDirections.actionProfileFragmentToFollowersFragment(caid)
        findNavController().safeNavigate(action)
    }

    private fun watchBroadcast() {
        val action = ProfileFragmentDirections.actionProfileFragmentToStageFragment(caid)
        findNavController().safeNavigate(action, NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, false).build())
    }
}

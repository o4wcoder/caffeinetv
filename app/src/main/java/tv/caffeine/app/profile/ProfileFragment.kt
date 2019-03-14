package tv.caffeine.app.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.databinding.FragmentProfileBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.FollowButtonDecorator
import tv.caffeine.app.ui.FollowButtonDecorator.Style
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.navigateToUnfollowUserDialog
import tv.caffeine.app.util.safeNavigate

class ProfileFragment : CaffeineFragment() {

    private val viewModel: ProfileViewModel by viewModels { viewModelFactory }
    private val args by navArgs<ProfileFragmentArgs>()
    private lateinit var caid: CAID
    private lateinit var binding: FragmentProfileBinding
    private var isFollowed: Boolean = false
    private val callback = object: FollowManager.Callback() {
        override fun follow(caid: CAID) {
            viewModel.follow(caid).observe(viewLifecycleOwner, Observer { result ->
                when(result) {
                    is CaffeineEmptyResult.Error -> {
                        if (result.error.isMustVerifyEmailError()) {
                            val fragment = AlertDialogFragment.withMessage(R.string.verify_email_to_follow_more_users)
                            fragment.maybeShow(fragmentManager, "verifyEmail")
                        } else {
                            Timber.e("Couldn't follow user ${result.error}")
                        }
                    }
                    is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
                }
            })
        }

        override fun unfollow(caid: CAID) {
            viewModel.unfollow(caid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        caid = args.caid
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel.load(caid)
        viewModel.username.observe(this, Observer { username ->
            binding.moreButton.apply {
                visibility = View.VISIBLE
                setOnClickListener { fragmentManager?.navigateToReportOrIgnoreDialog(caid, username, true) }
            }
            binding.followButton.setOnClickListener {
                if (isFollowed) {
                    fragmentManager?.navigateToUnfollowUserDialog(caid, username, callback)
                } else {
                    callback.follow(caid)
                }
            }
            binding.stageImageView.setOnClickListener { watchBroadcast(username) }
        })
        viewModel.isFollowed.observe(this, Observer { isFollowed ->
            this.isFollowed = isFollowed
            binding.followButton.apply {
                visibility = View.VISIBLE
                FollowButtonDecorator(if (isFollowed) Style.FOLLOWING else Style.FOLLOW).decorate(this)
            }
        })
        binding.profileViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.followingContainer.setOnClickListener { showFollowingList() }
        binding.followerContainer.setOnClickListener { showFollowersList() }
        return binding.root
    }

    private fun showFollowingList() {
        val action = ProfileFragmentDirections.actionProfileFragmentToFollowingFragment(caid)
        findNavController().safeNavigate(action)
    }

    private fun showFollowersList() {
        val action = ProfileFragmentDirections.actionProfileFragmentToFollowersFragment(caid)
        findNavController().safeNavigate(action)
    }

    private fun watchBroadcast(username: String) {
        findNavController().safeNavigate(ProfileFragmentDirections.actionProfileFragmentToStageFragment(username))
    }
}

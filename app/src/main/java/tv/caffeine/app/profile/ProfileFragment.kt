package tv.caffeine.app.profile


import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dagger.android.support.DaggerFragment
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentProfileBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var followManager: FollowManager
    private var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)

        val cropBorderedCircleTransformation = CropBorderedCircleTransformation(
                resources.getColor(R.color.caffeineBlue, null),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics))
        val cropCircleTransformation = CropCircleTransformation()
        val followedTheme = UserTheme(cropBorderedCircleTransformation, R.style.BroadcastCardUsername_Following)
        val notFollowedTheme = UserTheme(cropCircleTransformation, R.style.BroadcastCardUsername_NotFollowing)
        val caid = ProfileFragmentArgs.fromBundle(arguments).caid
        job = launch {
            val user = followManager.userDetails(caid)
            launch(UI) {
                user.configure(binding.avatarImageView, binding.usernameTextView, binding.followButton,
                        followManager, true, R.dimen.profile_size, followedTheme, notFollowedTheme)
                binding.nameTextView.text = user.name
                binding.numberFollowingTextView.text = user.followingCount.toString()
                binding.numberOfFollowersTextView.text = user.followersCount.toString()
                binding.bioTextView.text = user.bio
                binding.stageImageView.isVisible = false
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel()
    }
}

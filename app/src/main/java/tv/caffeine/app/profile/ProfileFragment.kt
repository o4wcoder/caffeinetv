package tv.caffeine.app.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentProfileBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var followManager: FollowManager
    @field:[Inject ThemeFollowedExplore] lateinit var followedTheme: UserTheme
    @field:[Inject ThemeNotFollowedExplore] lateinit var notFollowedTheme: UserTheme
    private var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)

        val caid = ProfileFragmentArgs.fromBundle(arguments).caid
        job = GlobalScope.launch(Dispatchers.Default) {
            val user = followManager.userDetails(caid)
            launch(Dispatchers.Main) {
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

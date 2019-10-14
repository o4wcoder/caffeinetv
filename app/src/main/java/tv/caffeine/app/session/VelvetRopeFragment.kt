package tv.caffeine.app.session

import android.os.Bundle
import android.view.View
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentVelvetRopeBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class VelvetRopeFragment @Inject constructor(
    val followManager: FollowManager
) : CaffeineFragment(R.layout.fragment_velvet_rope) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FragmentVelvetRopeBinding.bind(view).apply {
            configure(this)
        }
    }

    private fun configure(binding: FragmentVelvetRopeBinding) {
        val username = followManager.currentUserDetails()?.username
        if (username != null) {
            binding.title = getString(R.string.velvet_rope_title, username)
        } else {
            binding.title = getString(R.string.welcome_to_caffeine)
            Timber.e(Exception("No username available on VelvetRopeFragment"))
        }
    }
}

package tv.caffeine.app.stage

import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentAboutBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class AboutFragment @Inject constructor() : CaffeineFragment(R.layout.fragment_about) {

    private val viewModel: ProfileViewModel by viewModels { viewModelFactory }
    private val args by navArgs<AboutFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAboutBinding.bind(view)
        viewModel.forceLoad(args.caid).observe(viewLifecycleOwner, Observer { userProfile ->
            binding.aboutTextView.text = getBioText(userProfile)
        })
    }

    @VisibleForTesting
    fun getBioText(userProfile: UserProfile) =
        if (userProfile.bio.isNullOrEmpty()) {
            getString(R.string.stage_profile_empty_biography, userProfile.username)
        } else {
            userProfile.bio
        }
}
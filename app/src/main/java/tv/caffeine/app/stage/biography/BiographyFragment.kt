package tv.caffeine.app.stage.biography

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentBiographyBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class BiographyFragment @Inject constructor() : CaffeineFragment(R.layout.fragment_biography) {

    private val viewModel: ProfileViewModel by viewModels { viewModelFactory }
    private val args by navArgs<BiographyFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentBiographyBinding.bind(view)
        viewModel.forceLoad(args.caid)
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            binding.biographyTextView.text = userProfile.bio
        })
    }
}
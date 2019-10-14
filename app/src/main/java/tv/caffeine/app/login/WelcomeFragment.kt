package tv.caffeine.app.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentWelcomeBinding

import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.safeNavigate

class WelcomeFragment : CaffeineFragment(R.layout.fragment_welcome) {

    private lateinit var binding: FragmentWelcomeBinding
    private val viewModel: WelcomeViewModel by viewModels { viewModelFactory }
    private val args by navArgs<WelcomeFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentWelcomeBinding.bind(view)
        viewModel.email = args.email
        binding.viewModel = viewModel

        binding.letsGoButton.setOnClickListener {
            val navController = findNavController()
            val navOptions = NavOptions.Builder().setPopUpTo(navController.graph.id, true).build()
            navController.safeNavigate(R.id.main_nav, null, navOptions)
        }
    }
}

package tv.caffeine.app.settings.authentication

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentTwoStepAuthDoneBinding
import tv.caffeine.app.ui.CaffeineFragment

class TwoStepAuthDoneFragment : CaffeineFragment(R.layout.fragment_two_step_auth_done) {

    private lateinit var binding: FragmentTwoStepAuthDoneBinding
    private val viewModel: TwoStepAuthViewModel by navGraphViewModels(R.id.settings) { viewModelFactory }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentTwoStepAuthDoneBinding.bind(view)
        binding.doneButton.setOnClickListener {
            findNavController().popBackStack(R.id.settingsFragment, false)
        }
        viewModel.updateMfaEnabled(true)
    }
}

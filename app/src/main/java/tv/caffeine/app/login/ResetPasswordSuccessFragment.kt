package tv.caffeine.app.login

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentResetPasswordSuccessBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.navigateToLanding

class ResetPasswordSuccessFragment : CaffeineFragment(R.layout.fragment_reset_password_success) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentResetPasswordSuccessBinding.bind(view)
        binding.resetPasswordSuccessButton.setOnClickListener {
            findNavController().navigateToLanding()
        }
    }
}

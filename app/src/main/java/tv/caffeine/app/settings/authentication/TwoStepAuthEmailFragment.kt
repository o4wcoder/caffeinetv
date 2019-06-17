package tv.caffeine.app.settings.authentication

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentTwoStepAuthEmailBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.showKeyboard

class TwoStepAuthEmailFragment :
    CaffeineFragment(R.layout.fragment_two_step_auth_email) {

    private val args by navArgs<TwoStepAuthEmailFragmentArgs>()
    private lateinit var binding: FragmentTwoStepAuthEmailBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentTwoStepAuthEmailBinding.bind(view)
        binding.verificationMessageText.text = getString(R.string.two_step_auth_email_message, args.email)

        context?.showKeyboard(binding.verificationCodeEditText)
    }
}

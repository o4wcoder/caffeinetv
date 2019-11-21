package tv.caffeine.app.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentConfirmEmailBinding
import tv.caffeine.app.ui.CaffeineFragment

class ConfirmEmailFragment : CaffeineFragment(R.layout.fragment_confirm_email) {

    val viewModel: ConfirmEmailViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentConfirmEmailBinding.bind(view)
        binding.viewModel = viewModel
        val data = activity?.intent?.data ?: return exit()
        val code = data.getQueryParameter("code") ?: return exit()
        val caid = data.getQueryParameter("caid") ?: return exit()
        viewModel.load(code, caid)

        binding.continueButton.setOnClickListener {
            if (viewModel.isSuccess) {
                exit()
            } else {
                viewModel.resendEmail()
            }
        }
    }

    private fun exit() {
        findNavController().popBackStack()
    }
}

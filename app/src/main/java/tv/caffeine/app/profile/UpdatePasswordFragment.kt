package tv.caffeine.app.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.R
import tv.caffeine.app.api.currentPasswordErrorsString
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.passwordErrorsString
import tv.caffeine.app.databinding.FragmentUpdatePasswordBinding
import tv.caffeine.app.ui.CaffeineFragment

class UpdatePasswordFragment : CaffeineFragment(R.layout.fragment_update_password) {
    private lateinit var binding: FragmentUpdatePasswordBinding
    private val viewModel: UpdateProfileViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentUpdatePasswordBinding.bind(view)
        binding.updateButton.setOnClickListener {
            val currentPassword = binding.currentPasswordEditText.text.toString()
            val password1 = binding.password1EditText.text.toString()
            val password2 = binding.password2EditText.text.toString()
            viewModel.updatePassword(currentPassword, password1, password2).observe(viewLifecycleOwner, Observer { result ->
                when (result) {
                    is CaffeineResult.Success -> findNavController().navigateUp()
                    is CaffeineResult.Error -> onError(result)
                    is CaffeineResult.Failure -> handleFailure(result)
                }
            })
        }
    }

    private fun <T> onError(result: CaffeineResult.Error<T>) {
        val error = result.error
        binding.formErrorTextView.text = error.generalErrorsString
        binding.password1TextInputLayout.error = error.passwordErrorsString
        binding.currentPasswordTextInputLayout.error = error.currentPasswordErrorsString
        super.handleError(result)
    }
}

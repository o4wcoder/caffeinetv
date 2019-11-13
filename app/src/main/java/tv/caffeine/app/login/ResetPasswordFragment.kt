package tv.caffeine.app.login

import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.CaffeineConstants.QUERY_KEY_CODE
import tv.caffeine.app.R
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.isPasswordError
import tv.caffeine.app.api.isPasswordResetCodeError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.passwordErrorsString
import tv.caffeine.app.api.resetPasswordErrorString
import tv.caffeine.app.databinding.FragmentResetPasswordBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.safeNavigate

class ResetPasswordFragment : CaffeineFragment(R.layout.fragment_reset_password) {
    private lateinit var binding: FragmentResetPasswordBinding
    private val viewModel: ResetPasswordViewModel by viewModels { viewModelFactory }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val data = activity?.intent?.data ?: return exit()
        val code = data.getQueryParameter(QUERY_KEY_CODE) ?: return exit()

        binding = FragmentResetPasswordBinding.bind(view)
        binding.viewModel = viewModel
        binding.newPasswordEditTextLayout.afterTextChanged { viewModel.password = it }
        binding.confirmPasswordEditTextLayout.afterTextChanged { viewModel.confirmPassword = it }
        binding.confirmPasswordEditTextLayout.setOnActionGo { resetPassword(code) }

        binding.resetPasswordButton.setOnClickListener {
            clearErrors()
            if (viewModel.validatePasswords()) {
                resetPassword(code)
            } else {
                // Need to show the red bar under the edit text for an error, but the error message is just the hint
                binding.newPasswordEditTextLayout.showError(getString(R.string.reset_password_new_password_hint))
                binding.confirmPasswordEditTextLayout.showError(getString(R.string.reset_password_confirm_password_hint))
                binding.formErrorTextView.text = getString(R.string.reset_password_match_error)
            }
        }
    }

    private fun resetPassword(code: String) {
        viewModel.resetPassword(code).observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { result ->
                when (result) {
                    is CaffeineEmptyResult.Success ->
                        findNavController().safeNavigate(R.id.action_resetPasswordFragment_to_resetPasswordSuccessFragment)
                    is CaffeineEmptyResult.Error -> onError(result.error)
                    is CaffeineEmptyResult.Failure -> onFailure(result.throwable)
                }
            }
        })
    }

    private fun clearErrors() {
        binding.newPasswordEditTextLayout.clearError()
        binding.confirmPasswordEditTextLayout.clearError()
        binding.formErrorTextView.text = null
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        val errorMessages = listOfNotNull(
            error.generalErrorsString,
            error.passwordErrorsString,
            error.resetPasswordErrorString
        )
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            when {
                error.isPasswordError() -> {
                    binding.newPasswordEditTextLayout.showError(it)
                    binding.confirmPasswordEditTextLayout.showError(it)
                }
                error.isPasswordResetCodeError() -> {
                    binding.formErrorTextView.text = getString(R.string.reset_password_code_failure)
                }
                else -> binding.formErrorTextView.text = it
            }
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        binding.formErrorTextView.text = getString(R.string.reset_password_general_failure)
    }

    private fun exit() {
        findNavController().popBackStack()
    }
}

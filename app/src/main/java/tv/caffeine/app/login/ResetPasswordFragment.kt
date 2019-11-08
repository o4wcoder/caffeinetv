package tv.caffeine.app.login

import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import timber.log.Timber
import tv.caffeine.app.CaffeineConstants.QUERY_KEY_CODE
import tv.caffeine.app.R
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.resetPasswordErrorString
import tv.caffeine.app.databinding.FragmentResetPasswordBinding

import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.safeNavigate

class ResetPasswordFragment : CaffeineFragment(R.layout.fragment_reset_password) {
    private lateinit var binding: FragmentResetPasswordBinding
    private val viewModel: ResetPasswordViewModel by viewModels { viewModelFactory }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentResetPasswordBinding.bind(view)
        binding.viewModel = viewModel
        binding.currentPassswordEditTextLayout.afterTextChanged { viewModel.password = it }
        binding.confirmPasswordEditTextLayout.afterTextChanged { viewModel.confirmPassword = it }

        val data = activity?.intent?.data ?: return exit()
        val code = data.getQueryParameter(QUERY_KEY_CODE) ?: return exit()
        binding.resetPasswordButton.setOnClickListener {
            if (viewModel.validatePasswords()) {
                resetPassword(code)
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
                    is CaffeineEmptyResult.Failure -> Timber.e("Failure resetting password!! ${result.throwable.message}")
                }
            }
        })
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        val errorMessages = listOfNotNull(error.generalErrorsString, error.resetPasswordErrorString)
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            // TODO: Set the API error on the UI
        }
    }

    private fun exit() {
        findNavController().popBackStack()
    }
}

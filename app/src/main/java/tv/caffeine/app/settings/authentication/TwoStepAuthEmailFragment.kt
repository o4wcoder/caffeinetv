package tv.caffeine.app.settings.authentication

import android.os.Bundle
import android.text.style.URLSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.databinding.FragmentTwoStepAuthEmailBinding
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.configureEmbeddedLink
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showKeyboard

class TwoStepAuthEmailFragment : CaffeineFragment(R.layout.fragment_two_step_auth_email) {

    private val args by navArgs<TwoStepAuthEmailFragmentArgs>()
    @VisibleForTesting lateinit var binding: FragmentTwoStepAuthEmailBinding
    @VisibleForTesting val viewModel: TwoStepAuthViewModel by navGraphViewModels(R.id.settings) { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentTwoStepAuthEmailBinding.bind(view)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.verificationMessageText.apply {
            configureEmbeddedLink(
                R.string.two_step_auth_email_message, ::onResendEmailClick, args.email
            )
        }
        context?.showKeyboard(binding.verificationCodeEditText)
        binding.verificationCodeEditText.setOnAction(EditorInfo.IME_ACTION_NEXT) { viewModel.onVerificationCodeButtonClick() }

        viewModel.sendVerificationCodeUpdate.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { result ->
                when (result) {
                    is CaffeineEmptyResult.Success -> {
                        findNavController().safeNavigate(
                            ActionOnlyNavDirections(R.id.action_twoStepAuthEmail_to_twoStepAuthDoneFragment)
                        )
                    }
                    is CaffeineEmptyResult.Error -> {
                        Timber.e("Error sending verification code, ${result.error}")
                        showErrorDialog()
                    }
                    is CaffeineEmptyResult.Failure -> {
                        Timber.e(result.throwable, "Failure sending MFA verification code")
                        showErrorDialog()
                    }
                }
            }
        })
    }

    private fun onResendEmailClick(url: String?) = object : URLSpan(url) {
        override fun onClick(widget: View) {
            viewModel.sendMTAEmailCode()
        }
    }

    private fun showErrorDialog() {
        val fragment = AlertDialogFragment.withMessage(R.string.two_step_auth_email_code_error)
        fragment.maybeShow(fragmentManager, "mfaCodeError")
    }
}

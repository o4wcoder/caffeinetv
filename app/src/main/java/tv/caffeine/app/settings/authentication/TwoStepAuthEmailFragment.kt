package tv.caffeine.app.settings.authentication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.databinding.FragmentTwoStepAuthEmailBinding
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.convertLinks
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showKeyboard

private const val MIN_CODE_LENGTH = 1

class TwoStepAuthEmailFragment : CaffeineFragment(R.layout.fragment_two_step_auth_email) {

    private val args by navArgs<TwoStepAuthEmailFragmentArgs>()
    @VisibleForTesting lateinit var binding: FragmentTwoStepAuthEmailBinding
    @VisibleForTesting val viewModel: TwoStepAuthEmailFragmentViewModel by viewModels { viewModelFactory }
    private var verificationCode = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentTwoStepAuthEmailBinding.bind(view)
        binding.verificationMessageText.apply {
            text = convertLinks(R.string.two_step_auth_email_message, resources, ::onResendEmailClick, args.email)
            movementMethod = LinkMovementMethod.getInstance()
        }
        context?.showKeyboard(binding.verificationCodeEditText)

        binding.verificationCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.verificationCodeButton.isEnabled = s.length >= MIN_CODE_LENGTH
                verificationCode = s.toString()
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.verificationCodeButton.setOnClickListener {
            viewModel.sendVerificationCode(verificationCode).observe(viewLifecycleOwner, Observer { event ->
                event.getContentIfNotHandled()?.let { result ->
                    when (result) {
                        is CaffeineEmptyResult.Success -> {
                            findNavController().safeNavigate(
                                ActionOnlyNavDirections(R.id.action_twoStepAuthEmail_to_twoStepAuthDoneFragment))
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

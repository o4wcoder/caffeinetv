package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.UiThread
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.databinding.FragmentForgotBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class ForgotFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var gson: Gson

    private lateinit var binding: FragmentForgotBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentForgotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.sendEmailButton.setOnClickListener { sendForgotPasswordEmail() }
        binding.emailEditText.setOnAction(EditorInfo.IME_ACTION_SEND) { sendForgotPasswordEmail() }
    }

    private fun sendForgotPasswordEmail() {
        clearErrorMessages()
        launch {
            val email = binding.emailEditText.text.toString()
            val result = accountsService.forgotPassword(ForgotPasswordBody(email)).awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> {
                    activity?.showSnackbar(R.string.forgot_password_email_sent)
                    findNavController().navigateUp()
                }
                is CaffeineEmptyResult.Error -> onError(result.error)
                is CaffeineEmptyResult.Failure -> onFailure(result.throwable)
            }
        }
    }

    private fun clearErrorMessages() {
        binding.formErrorTextView.text = null
        binding.emailTextInputLayout.error = null
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        binding.formErrorTextView.text = error.generalErrorsString
        binding.emailTextInputLayout.error = error.emailErrorsString
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t)
        binding.formErrorTextView.setText(R.string.forgot_password_cannot_reset_password)
    }

}

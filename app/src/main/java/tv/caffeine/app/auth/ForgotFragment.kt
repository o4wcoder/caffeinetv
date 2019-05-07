package tv.caffeine.app.auth


import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.UiThread
import androidx.core.view.isInvisible
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.ForgotPasswordBody
import tv.caffeine.app.api.emailErrorsString
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.databinding.FragmentForgotBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class ForgotFragment @Inject constructor(
        private val accountsService: AccountsService,
        private val gson: Gson
) : CaffeineFragment(R.layout.fragment_forgot) {

    private lateinit var binding: FragmentForgotBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentForgotBinding.bind(view)
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
                    activity?.showSnackbar(R.string.email_sent_message)
                    findNavController().navigateUp()
                }
                is CaffeineEmptyResult.Error -> onError(result.error)
                is CaffeineEmptyResult.Failure -> onFailure(result.throwable)
            }
        }
    }

    private fun clearErrorMessages() {
        binding.formErrorTextView.isInvisible = true
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        val errorMessages = listOfNotNull(error.generalErrorsString, error.emailErrorsString)
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            binding.formErrorTextView.text = it
            binding.formErrorTextView.isInvisible = false
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "reset your password failure")
        showSnackbar(R.string.reset_your_password_failure)
    }
}

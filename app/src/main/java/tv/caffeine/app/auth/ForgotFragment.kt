package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.ForgotPasswordBody
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentForgotBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class ForgotFragment : CaffeineFragment() {
    @Inject lateinit var accountsService: AccountsService

    private lateinit var binding: FragmentForgotBinding

    @Inject lateinit var gson: Gson

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentForgotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendEmailButton.setOnClickListener { sendForgotPasswordEmail() }
    }

    private fun sendForgotPasswordEmail() {
        clearErrorMessages()
        launch {
            val email = binding.emailEditText.text.toString()
            val result = accountsService.forgotPassword(ForgotPasswordBody(email)).awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> findNavController().navigateUp()
                is CaffeineResult.Error -> onError(result.error)
                is CaffeineResult.Failure -> onFailure(result.exception)
            }
        }
    }

    private fun clearErrorMessages() {
        binding.formErrorTextView.text = null
        binding.emailTextInputLayout.error = null
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        error.errors._error?.joinToString("\n")?.let { binding.formErrorTextView.text = it }
        error.errors.email?.joinToString("\n")?.let { binding.emailTextInputLayout.error = it }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "Something went wrong trying to request forgot password email") // TODO: handle error
    }

}

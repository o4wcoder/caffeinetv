package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ConfirmEmailBody
import tv.caffeine.app.api.isRecordNotFoundError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class ConfirmEmailFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var gson: Gson

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirm_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val data = activity?.intent?.data ?: return exit()
        val code = data.getQueryParameter("code") ?: return exit()
        val caid = data.getQueryParameter("caid") ?: return exit()
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        continueButton.setOnClickListener { exit() }
        launch {
            val body = ConfirmEmailBody(code, caid)
            val result = accountsService.confirmEmail(body).awaitAndParseErrors(gson)
            val statusTextView = view.findViewById<TextView>(R.id.subtitle_text_view)
            val statusStringResId = when (result) {
                is CaffeineResult.Success -> R.string.email_verification_success
                is CaffeineResult.Error -> if (result.error.isRecordNotFoundError()) R.string.email_verification_record_not_found else R.string.email_verification_failed
                is CaffeineResult.Failure -> R.string.email_verification_failed
            }
            statusTextView.setText(statusStringResId)
            statusTextView.isVisible = true
            continueButton.isVisible = true
        }
    }

    private fun exit() {
        findNavController().popBackStack()
    }
}
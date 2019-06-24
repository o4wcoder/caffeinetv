package tv.caffeine.app.settings.authentication

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.SendAccountMFABody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.MfaMethod
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.ui.CaffeineDialogFragment
import javax.inject.Inject

class TwoStepAuthDisableDialogFragment : CaffeineDialogFragment() {

    private val viewModel: DisableTwoStepAuthenticationViewModel by viewModels { viewModelFactory }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }

        return AlertDialog.Builder(activity)
            .setMessage(R.string.two_step_auth_dialog_off_message)
            .setPositiveButton(R.string.two_step_auth_dialog_off_disable_button) { _, _ -> disableAuth() }
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .create()
    }

    private fun disableAuth() {
        viewModel.disableAuth()
        dismiss()
        findNavController().popBackStack(R.id.settingsFragment, false)
    }
}

class DisableTwoStepAuthenticationViewModel @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson
) : ViewModel() {

    fun disableAuth() {

        viewModelScope.launch {
            val result = accountsService.setMFA(SendAccountMFABody(SendAccountMFABody.Mfa(MfaMethod.NONE, "")))
                .awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> Timber.d("Success disabling Two-Step Authentication")
                is CaffeineEmptyResult.Error -> Timber.e("Error attempting to disable Two-Step Authentication, ${result.error}")
                is CaffeineEmptyResult.Failure -> Timber.e(result.throwable, "Failed to to disable Two-Step Authentication")
            }
        }
    }
}
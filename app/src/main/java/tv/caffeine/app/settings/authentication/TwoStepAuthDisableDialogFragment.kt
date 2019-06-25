package tv.caffeine.app.settings.authentication

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.navigation.navGraphViewModels
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.ui.CaffeineDialogFragment

class TwoStepAuthDisableDialogFragment : CaffeineDialogFragment() {

    private val viewModel: TwoStepAuthViewModel by navGraphViewModels(R.id.settings) { viewModelFactory }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }

        return AlertDialog.Builder(activity)
            .setMessage(R.string.two_step_auth_dialog_off_message)
            .setPositiveButton(R.string.two_step_auth_dialog_off_disable_button) { _, _ -> viewModel.disableAuth() }
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .create()
    }
}
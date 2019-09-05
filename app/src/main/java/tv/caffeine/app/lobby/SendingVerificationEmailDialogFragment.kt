package tv.caffeine.app.lobby

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import timber.log.Timber
import tv.caffeine.app.R

class SendingVerificationEmailDialogFragment : DialogFragment() {
    private val args by navArgs<SendingVerificationEmailDialogFragmentArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }

        return AlertDialog.Builder(activity)
                .setTitle(R.string.sending_verification_email_title)
                .setMessage(getString(R.string.sending_verification_email_message, args.email))
                .setPositiveButton(R.string.ok) { _, _ -> dismiss() }
                .create()
    }
}

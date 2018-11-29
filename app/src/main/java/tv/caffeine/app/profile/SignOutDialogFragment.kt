package tv.caffeine.app.profile

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import timber.log.Timber
import tv.caffeine.app.R
import java.lang.IllegalStateException

class SignOutDialogFragment : DialogFragment() {
    var positiveClickListener: DialogInterface.OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        return AlertDialog.Builder(activity)
                .setTitle(R.string.sign_out_question)
                .setPositiveButton(R.string.sign_out_button, positiveClickListener)
                .setNegativeButton(R.string.cancel ) { _, _ -> dismiss() }
                .create()
    }
}

package tv.caffeine.app.broadcast

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import timber.log.Timber
import tv.caffeine.app.R

class BroadcastPlaceholderDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        return AlertDialog.Builder(activity)
                .setTitle(R.string.broardcast_placeholder_dialog_title)
                .setMessage(R.string.broardcast_placeholder_dialog_message)
                .setPositiveButton(R.string.got_it, null)
                .create()
    }
}
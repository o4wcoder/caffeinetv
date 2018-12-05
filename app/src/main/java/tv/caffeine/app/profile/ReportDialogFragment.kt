package tv.caffeine.app.profile

import tv.caffeine.app.R
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import timber.log.Timber

class ReportDialogFragment : DialogFragment()
{
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        val args = ReportDialogFragmentArgs.fromBundle(arguments)
        val caid = args.caid
        val username = args.username
        // TODO (david) implemented in the next PR.
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.report_user, username))
                .create()
    }
}
package tv.caffeine.app.profile

import tv.caffeine.app.R
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import timber.log.Timber

class ReportOrIgnoreDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        val args=  ReportOrIgnoreDialogFragmentArgs.fromBundle(arguments)
        val caid = args.caid
        val username = args.username
        val text = arrayOf(
                getString(R.string.report_user_more, username),
                getString(R.string.ignore_user, username)
        )
        return AlertDialog.Builder(activity)
                .setItems(text) { _, which ->
                    when (which) {
                        0 -> reportUser(caid, username)
                        1 -> ignoreUser(caid)
                    }
                }.create()
    }

    private fun reportUser(caid: String, username: String) {
        fragmentManager?.let { fm ->
            ReportDialogFragment().apply {
                arguments = ReportOrIgnoreDialogFragmentDirections
                        .actionProfileFragmentToReportDialogFragment(caid, username).arguments
                show(fm, "reportUser")
            }
        }
    }

    private fun ignoreUser(caid: String) {
        // TODO (david) implemented in the next PR.
    }
}
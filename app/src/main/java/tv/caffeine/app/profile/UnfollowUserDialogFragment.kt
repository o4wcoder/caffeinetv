package tv.caffeine.app.profile

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import timber.log.Timber
import tv.caffeine.app.R

class UnfollowUserDialogFragment : DialogFragment() {
    var positiveClickListener: DialogInterface.OnClickListener? = null
    private val args by navArgs<UnfollowUserDialogFragmentArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        val username = args.username

        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.unfollow_question, username))
                .setPositiveButton(R.string.unfollow_button, positiveClickListener)
                .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
                .create()
    }
}

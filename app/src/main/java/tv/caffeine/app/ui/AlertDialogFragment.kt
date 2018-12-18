package tv.caffeine.app.ui

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import timber.log.Timber
import tv.caffeine.app.R

private const val MESSAGE_STRING_RES_ID = "MESSAGE_STRING_RES_ID"

class AlertDialogFragment : DialogFragment() {

    companion object {
        fun withMessage(@StringRes messageResId: Int) =
                AlertDialogFragment().apply {
                    arguments = Bundle().apply {
                        putInt(MESSAGE_STRING_RES_ID, messageResId)
                    }
                }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = this.activity ?: IllegalStateException("Activity cannot be null").let {
            Timber.e(it)
            throw it
        }
        val messageResId = arguments?.getInt(MESSAGE_STRING_RES_ID) ?: IllegalArgumentException("message missing").let {
            Timber.e(it)
            throw it
        }
        return AlertDialog.Builder(activity)
                .setTitle(messageResId)
                .setPositiveButton(R.string.got_it, null)
                .create()
    }
}


package tv.caffeine.app.ui

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import timber.log.Timber
import tv.caffeine.app.R

private const val MESSAGE_STRING_RES_ID = "MESSAGE_STRING_RES_ID"
private const val USER_EMAIL_VERIFY_SUCCESS = "USER_EMAIL_VERIFY_SUCCESS"
private const val DIALOG_TYPE = "DIALOG_TYPE"

class AlertDialogFragment : DialogFragment() {

    interface Callbacks {
        fun onResendEmail()
    }

    enum class Type {
        Message, Verify, VerifySuccess;

        companion object {
            private val map = values().associateBy(Type::ordinal)
            fun fromOrdinal(type: Int?) = map[type]
        }
    }

    companion object {
        fun withMessage(@StringRes messageResId: Int) =
                AlertDialogFragment().apply {
                    arguments = Bundle().apply {
                        putInt(DIALOG_TYPE, Type.Message.ordinal)
                        putInt(MESSAGE_STRING_RES_ID, messageResId)
                    }
                }

        fun verifyEmailWithMessage(@StringRes messageResId: Int) =
            AlertDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(DIALOG_TYPE, Type.Verify.ordinal)
                    putInt(MESSAGE_STRING_RES_ID, messageResId)
                }
            }

        fun withEmailForSuccess(emailAddress: String) =
            AlertDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(DIALOG_TYPE, Type.VerifySuccess.ordinal)
                    putString(USER_EMAIL_VERIFY_SUCCESS, emailAddress)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = this.activity ?: IllegalStateException("Activity cannot be null").let {
            Timber.e(it)
            throw it
        }
        val messageType = Type.fromOrdinal(arguments?.getInt(DIALOG_TYPE)) ?: IllegalStateException("Type must exist").let {
            Timber.e(it)
            throw it
        }

        return when (messageType) {
            Type.Message -> {
                val messageResId = arguments?.getInt(MESSAGE_STRING_RES_ID) ?: IllegalArgumentException("message missing").let {
                    Timber.e(it)
                    throw it
                }
                AlertDialog.Builder(activity)
                    .setTitle(messageResId)
                    .setPositiveButton(R.string.got_it, null)
                    .create()
            }
            Type.Verify -> {
                val messageResId = arguments?.getInt(MESSAGE_STRING_RES_ID) ?: IllegalArgumentException("message missing").let {
                    Timber.e(it)
                    throw it
                }
                AlertDialog.Builder(activity)
                    .setTitle(R.string.verify_email_dialog_title)
                    .setMessage(messageResId)
                    .setPositiveButton(R.string.resend_email_dialog_button) { _, _ ->
                        if (targetFragment is Callbacks) (targetFragment as Callbacks).onResendEmail()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .create()
            }
            Type.VerifySuccess -> {
                val emailAddress = arguments?.getString(USER_EMAIL_VERIFY_SUCCESS) ?: IllegalArgumentException("email address missing").let {
                    Timber.e(it)
                    throw it
                }
                AlertDialog.Builder(activity)
                    .setTitle(R.string.verify_email_success_dialog_title)
                    .setMessage(resources.getString(R.string.sending_verification_email_message, emailAddress))
                    .setPositiveButton(R.string.ok, null)
                    .create()
            }
        }
    }
}

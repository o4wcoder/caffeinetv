package tv.caffeine.app.settings

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.ui.CaffeineDialogFragment

class DisconnectIdentityDialogFragment : CaffeineDialogFragment() {

    interface Callback {
        fun confirmDisconnectIdentity(socialUid: String, identityProvider: IdentityProvider)
    }

    private val args by navArgs<DisconnectIdentityDialogFragmentArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = this.activity ?: IllegalStateException("Activity cannot be null").let {
            Timber.e(it)
            throw it
        }
        val callback = targetFragment as? Callback ?: IllegalArgumentException("Target fragment must implement Callback").let {
            Timber.e(it)
            throw it
        }
        val socialUid = args.socialUid
        val identityProvider = args.identityProvider
        val displayName = args.displayName
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.disconnect_identity, displayName))
                .setPositiveButton(R.string.disconnect) { _, _ -> callback.confirmDisconnectIdentity(socialUid, identityProvider) }
                .setNegativeButton(R.string.cancel, null)
                .create()
    }
}

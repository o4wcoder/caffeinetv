package tv.caffeine.app.settings

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.ui.CaffeineDialogFragment
import java.text.NumberFormat

class BuyGoldUsingCreditsDialogFragment : CaffeineDialogFragment() {

    interface Callback {
        fun buyGoldBundle(goldBundleId: String)
    }

    private val args by navArgs<BuyGoldUsingCreditsDialogFragmentArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = this.activity ?: IllegalStateException("Activity cannot be null").let {
            Timber.e(it)
            throw it
        }
        val callback = targetFragment as? Callback ?: IllegalArgumentException("Target fragment must implement Callback").let {
            Timber.e(it)
            throw it
        }
        val goldBundleId = args.goldBundleId
        val gold = args.gold
        val credits = args.credits
        val numberFormat = NumberFormat.getInstance()
        val alert = AlertDialog.Builder(activity)
                .setTitle(R.string.buy_gold)
                .setMessage(getString(R.string.buy_gold_for_credits, numberFormat.format(gold), numberFormat.format(credits)))
                .setPositiveButton(R.string.buy) { _, _ -> callback.buyGoldBundle(goldBundleId) }
                .setNegativeButton(R.string.cancel, null)
                .create()
        return alert
    }
}

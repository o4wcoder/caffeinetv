package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.squareup.picasso.Picasso
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentGoldAndCreditsBinding
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.wallet.WalletViewModel
import java.text.NumberFormat
import javax.inject.Inject

@Parcelize
enum class BuyGoldOption: Parcelable {
    UsingPlayStore, UsingCredits
}

class GoldAndCreditsFragment : CaffeineFragment() {
    @Inject lateinit var featureConfig: FeatureConfig
    @Inject lateinit var picasso: Picasso
    private lateinit var binding: FragmentGoldAndCreditsBinding
    private val walletViewModel by lazy { viewModelProvider.get(WalletViewModel::class.java) }
    private val goldBundlesViewModel by lazy { viewModelProvider.get(GoldBundlesViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentGoldAndCreditsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        binding.walletViewModel = walletViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val numberFormat = NumberFormat.getIntegerInstance()
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer {  wallet ->
            binding.goldBalanceTextView.formatUsernameAsHtml(picasso, getString(R.string.gold_formatted, numberFormat.format(wallet.gold)))
            binding.creditBalanceTextView.formatUsernameAsHtml(picasso, getString(R.string.credits_formatted, numberFormat.format(wallet.credits)))
            binding.cumulativeCreditBalanceTextView.formatUsernameAsHtml(picasso, getString(R.string.credits_formatted, numberFormat.format(wallet.cumulativeCredits)))
        })
        binding.transactionHistoryButton.setOnClickListener {
            val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToTransactionHistoryFragment()
            findNavController().safeNavigate(action)
        }
        binding.buyGoldButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingPlayStore) }
        binding.buyGoldWithCreditsButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingCredits) }
        binding.goldAndCreditsHelpTextView.formatUsernameAsHtml(picasso, getString(R.string.gold_and_credits_help_html))
    }

    override fun onResume() {
        super.onResume()
        if (featureConfig.isFeatureEnabled(Feature.PAYMENT_FIX)) {
            processRecentlyCachedPurchases()
        }
    }

    private fun navigateToBuyGold(buyGoldOption: BuyGoldOption) {
        val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToGoldBundlesFragment(buyGoldOption)
        findNavController().safeNavigate(action)
    }

    /**
     * We will redeem cached google play payments as a short-term fix for the bug that
     * a network or Caffeine error occurred when we processed the payment.
     */
    private fun processRecentlyCachedPurchases() {
        val context = context ?: return
        val count = 5 // Only re-process the most recent 5 purchases
        val billingClient = BillingClientFactory.createBillingClient(context, PurchasesUpdatedListener { _, _ -> })
        billingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Timber.e("Billing service disconnected")
            }

            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    Timber.d("Successfully started billing connection")
                    val purchaseResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
                    if (purchaseResult.responseCode == BillingClient.BillingResponse.OK) {
                        for (purchase in purchaseResult.purchasesList.takeLast(count)) {
                            goldBundlesViewModel.processInAppPurchase(purchase)
                        }
                    }
                } else {
                    Timber.e("Failed to start billing connection")
                }
            }
        })
        billingClient.endConnection()
    }
}

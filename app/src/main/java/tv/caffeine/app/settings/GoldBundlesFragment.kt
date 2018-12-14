package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.android.billingclient.api.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.databinding.FragmentGoldBundlesBinding
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.htmlText
import java.text.NumberFormat

class GoldBundlesFragment : CaffeineFragment() {

    private lateinit var binding: FragmentGoldBundlesBinding
    private val viewModel by lazy { viewModelProvider.get(GoldBundlesViewModel::class.java) }
    private val walletViewModel by lazy { viewModelProvider.get(WalletViewModel::class.java) }
    private val goldBundlesAdapter = GoldBundlesAdapter(object : GoldBundleClickListener {
        override fun onClick(goldBundle: GoldBundle) {
            purchaseGoldBundle(goldBundle)
        }
    })

    private lateinit var billingClient: BillingClient
    private val buyGoldOption by lazy { GoldBundlesFragmentArgs.fromBundle(arguments).buyGoldOption }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureBillingClient()
    }

    private fun configureBillingClient() {
        if (buyGoldOption != BuyGoldOption.UsingPlayStore) return
        val activity = activity ?: return
        billingClient = BillingClientFactory.createBillingClient(activity, PurchasesUpdatedListener { responseCode, purchases ->
            Timber.d("Connected")
            if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
                purchases.forEach {
                    // TODO post to server
                    Timber.d("Purchased ${it.sku}: ${it.orderId}, ${it.purchaseToken}")
                }
            } else {
                Timber.d("Failed to make a purchase $responseCode")
            }
        })
        billingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    Timber.d("Successfully started billing connection")
                } else {
                    Timber.d("Failed to start billing connection")
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentGoldBundlesBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(viewLifecycleOwner)
            goldBundlesRecyclerView.adapter = goldBundlesAdapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer { wallet ->
            val goldCount = NumberFormat.getIntegerInstance().format(wallet.gold)
            val creditBalance = NumberFormat.getIntegerInstance().format(wallet.credits)
            binding.currentBalanceTextView.htmlText = when(buyGoldOption) {
                BuyGoldOption.UsingCredits -> getString(R.string.you_have_gold_and_credits_balance, goldCount, creditBalance)
                BuyGoldOption.UsingPlayStore -> getString(R.string.you_have_gold_balance, goldCount)
            }
        })
        val handler = Handler()
        viewModel.goldBundles.observe(viewLifecycleOwner, Observer { result ->
            handle(result, view) { paymentsEnvelope ->
                val list = paymentsEnvelope.payload.goldBundles.state.filter { it.usingInAppBilling != null }
                val skuList = list.mapNotNull { it.usingInAppBilling }.map { it.productId }
                when(buyGoldOption) {
                    BuyGoldOption.UsingCredits -> goldBundlesAdapter.submitList(list)
                    BuyGoldOption.UsingPlayStore -> {
                        val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(BillingClient.SkuType.INAPP).build()
                        billingClient.querySkuDetailsAsync(params) { responseCode, skuDetailsList ->
                            Timber.d("Results: $skuDetailsList")
                            list.forEach {  goldBundle ->
                                goldBundle.skuDetails = skuDetailsList.find { it.sku == goldBundle.usingInAppBilling?.productId }
                            }
                            handler.post { goldBundlesAdapter.submitList(list) }
                        }
                    }
                }
            }
        })
    }

    private fun purchaseGoldBundle(goldBundle: GoldBundle) {
        when(buyGoldOption) {
            BuyGoldOption.UsingCredits -> promptPurchaseGoldBundleUsingCredits(goldBundle)
            BuyGoldOption.UsingPlayStore -> purchaseGoldBundleUsingPlayStore(goldBundle)
        }
    }

    private fun promptPurchaseGoldBundleUsingCredits(goldBundle: GoldBundle) {
        val context = context ?: return
        val alertDialog = AlertDialog.Builder(context)
                .setTitle("Buy Gold")
                .setMessage("${goldBundle.amount} for ${goldBundle.usingCredits?.cost}")
                .setPositiveButton("Buy") { _, _ -> purchaseGoldBundleUsingCredits(goldBundle) }
                .setNegativeButton("Cancel") { _, _ -> }
                .create()
        alertDialog.show()
    }

    private fun purchaseGoldBundleUsingCredits(goldBundle: GoldBundle) {
        viewModel.purchaseGoldBundleUsingCredits(goldBundle)
    }

    private fun purchaseGoldBundleUsingPlayStore(goldBundle: GoldBundle) {
        val activity = activity ?: return
        val sku = goldBundle.skuDetails?.sku ?: return
        val params = BillingFlowParams.newBuilder().setSku(sku).setType(BillingClient.SkuType.INAPP).build()
        billingClient.launchBillingFlow(activity, params)
    }

}

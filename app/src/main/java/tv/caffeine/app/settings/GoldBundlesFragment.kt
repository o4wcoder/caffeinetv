package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.android.billingclient.api.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentGoldBundlesBinding
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.htmlText
import tv.caffeine.app.util.showSnackbar
import java.text.NumberFormat
import javax.inject.Inject

class GoldBundlesFragment : CaffeineBottomSheetDialogFragment(), BuyGoldUsingCreditsDialogFragment.Callback {

    @Inject lateinit var tokenStore: TokenStore

    private lateinit var binding: FragmentGoldBundlesBinding
    private val viewModel by lazy { viewModelProvider.get(GoldBundlesViewModel::class.java) }
    private val goldBundlesAdapter by lazy {
        GoldBundlesAdapter(buyGoldOption, object : GoldBundleClickListener {
            override fun onClick(goldBundle: GoldBundle) {
                purchaseGoldBundle(goldBundle)
            }
        })
    }

    private var availableCredits = 0
    private lateinit var billingClient: BillingClient
    private val args by navArgs<GoldBundlesFragmentArgs>()
    private val buyGoldOption by lazy { args.buyGoldOption }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureBillingClient()
    }

    private fun configureBillingClient() {
        if (buyGoldOption != BuyGoldOption.UsingPlayStore) return
        val activity = activity ?: return
        billingClient = BillingClientFactory.createBillingClient(activity, PurchasesUpdatedListener { responseCode, purchases ->
            Timber.d("Connected")
            consumeInAppPurchases(responseCode, purchases)
        })
        billingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Timber.e("Billing service disconnected")
            }

            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    Timber.d("Successfully started billing connection")
                } else {
                    Timber.e("Failed to start billing connection")
                }
            }
        })
    }

    private fun consumeInAppPurchases(responseCode: Int, purchases: List<Purchase>?) {
        when {
            responseCode == BillingClient.BillingResponse.OK && purchases != null -> {
                for (purchase in purchases) {
                    consumeInAppPurchase(purchase)
                }
            }
            responseCode == BillingClient.BillingResponse.USER_CANCELED -> {
                Timber.d("User canceled")
                activity?.showSnackbar(R.string.user_cancel_buying_gold_using_in_app_billing)
            }
            else -> {
                Timber.e("Billing client error $responseCode")
                activity?.showSnackbar(R.string.failure_buying_gold_using_in_app_billing)
            }
        }
    }

    private fun consumeInAppPurchase(purchase: Purchase) {
        try {
            billingClient.consumeAsync(purchase.purchaseToken) { responseCode, purchaseToken ->
                when (responseCode) {
                    BillingClient.BillingResponse.OK -> {
                        processInAppPurchase(purchase)
                        Timber.d("Successfully consumed the purchase $purchaseToken")
                    }
                    BillingClient.BillingResponse.USER_CANCELED -> {
                        Timber.d("User canceled purchase")
                        activity?.showSnackbar(R.string.user_cancel_buying_gold_using_in_app_billing)
                    }
                    else -> {
                        Timber.e("Failed to consume the purchase $purchaseToken")
                        activity?.showSnackbar(R.string.failure_buying_gold_using_in_app_billing)
                    }
                }
            }
        } catch (nee: NotImplementedError) {
            // This will happen when using BillingX testing library,
            // since v 0.8.0 doesn't implement consuming IAB purchases
            Timber.d("Debug build, using BillingX library, consuming purchases isn't supported")
            processInAppPurchase(purchase)
        } catch (t: Throwable) {
            Timber.e(t)
        }
    }

    private fun processInAppPurchase(purchase: Purchase) {
        viewModel.processInAppPurchase(purchase).observe(this@GoldBundlesFragment, Observer { purchaseStatus ->
            when(purchaseStatus.result) {
                is CaffeineResult.Success -> {
                    Timber.d("Successfully processed purchase")
                    activity?.showSnackbar(R.string.success_buying_gold_using_in_app_billing)
                }
                else -> {
                    Timber.e("Failed to process purchase")
                    activity?.showSnackbar(R.string.failure_processing_in_app_purchase)
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentGoldBundlesBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(viewLifecycleOwner)
            goldBundlesRecyclerView.adapter = goldBundlesAdapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.wallet.observe(viewLifecycleOwner, Observer { wallet ->
            availableCredits = wallet.credits
            val goldCount = NumberFormat.getIntegerInstance().format(wallet.gold)
            val creditBalance = NumberFormat.getIntegerInstance().format(wallet.credits)
            binding.currentBalanceTextView.htmlText = when(buyGoldOption) {
                BuyGoldOption.UsingCredits -> getString(R.string.you_have_gold_and_credits_balance, goldCount, creditBalance)
                BuyGoldOption.UsingPlayStore -> getString(R.string.you_have_gold_balance, goldCount)
            }
        })
        val handler = Handler()
        viewModel.goldBundles.observe(viewLifecycleOwner, Observer { result ->
            when(result) {
                is CaffeineResult.Success -> {
                    val goldBundles = result.value
                    val list = goldBundles.filter { it.usingInAppBilling != null }
                    val skuList = list.mapNotNull { it.usingInAppBilling }.map { it.productId }
                    when (buyGoldOption) {
                        BuyGoldOption.UsingCredits -> goldBundlesAdapter.submitList(list)
                        BuyGoldOption.UsingPlayStore -> {
                            val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(BillingClient.SkuType.INAPP).build()
                            billingClient.querySkuDetailsAsync(params) { responseCode, skuDetailsList ->
                                if (responseCode != BillingClient.BillingResponse.OK) {
                                    Timber.e("Error loading SKU details, $responseCode")
                                    return@querySkuDetailsAsync
                                }
                                Timber.d("Results: $skuDetailsList")
                                list.forEach { goldBundle ->
                                    goldBundle.skuDetails = skuDetailsList.find { it.sku == goldBundle.usingInAppBilling?.productId }
                                }
                                handler.post { goldBundlesAdapter.submitList(list) }
                            }
                        }
                    }
                }
                else -> activity?.showSnackbar(R.string.error_loading_gold_bundles)
            }
        })
    }

    private fun purchaseGoldBundle(goldBundle: GoldBundle) {
        // TODO wallet balance check
        when(buyGoldOption) {
            BuyGoldOption.UsingCredits -> {
                if (goldBundle.usingCredits?.canPurchase == true ) {
                    if (availableCredits >= goldBundle.usingCredits.cost) {
                        promptPurchaseGoldBundleUsingCredits(goldBundle)
                    } else {
                        AlertDialogFragment.withMessage(R.string.cannot_purchase_not_enough_credits).show(fragmentManager, "cannotPurchase")
                    }
                } else {
                    AlertDialogFragment.withMessage(R.string.cannot_purchase_using_credits).show(fragmentManager, "cannotPurchase")
                }
            }
            BuyGoldOption.UsingPlayStore -> {
                if (goldBundle.usingInAppBilling?.canPurchase == true) {
                    purchaseGoldBundleUsingPlayStore(goldBundle)
                } else {
                    AlertDialogFragment.withMessage(R.string.cannot_purchase_using_play_store).show(fragmentManager, "cannotPurchase")
                }
            }
        }
    }

    private fun promptPurchaseGoldBundleUsingCredits(goldBundle: GoldBundle) {
        val usingCredits = goldBundle.usingCredits ?: return
        val goldBundleId = usingCredits.id
        val gold = goldBundle.amount
        val credits = usingCredits.cost
        val action = GoldBundlesFragmentDirections.actionGoldBundlesFragmentToBuyGoldUsingCreditsDialogFragment(goldBundleId, gold, credits)
        val fragment = BuyGoldUsingCreditsDialogFragment()
        fragment.arguments = action.arguments
        fragment.setTargetFragment(this, 0)
        fragment.show(fragmentManager, "buyGoldUsingCredits")
    }

    override fun buyGoldBundle(goldBundleId: String) {
        viewModel.purchaseGoldBundleUsingCredits(goldBundleId).observe(viewLifecycleOwner, Observer { success ->
            activity?.showSnackbar(if (success) R.string.success_buying_gold_using_credits else R.string.error_buying_gold_using_credits)
        })
    }

    private fun purchaseGoldBundleUsingPlayStore(goldBundle: GoldBundle) {
        val activity = activity ?: return
        val sku = goldBundle.skuDetails?.sku ?: return
        val params = BillingFlowParams.newBuilder().setSku(sku).setAccountId(tokenStore.caid).setType(BillingClient.SkuType.INAPP).build()
        billingClient.launchBillingFlow(activity, params)
    }

}

package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.android.billingclient.api.*
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.databinding.FragmentGoldBundlesBinding
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.htmlText
import tv.caffeine.app.util.showSnackbar
import java.text.NumberFormat

class GoldBundlesFragment : CaffeineBottomSheetDialogFragment(), BuyGoldUsingCreditsDialogFragment.Callback {

    private lateinit var binding: FragmentGoldBundlesBinding
    private val viewModel by lazy { viewModelProvider.get(GoldBundlesViewModel::class.java) }
    private val goldBundlesAdapter by lazy {
        GoldBundlesAdapter(buyGoldOption, object : GoldBundleClickListener {
            override fun onClick(goldBundle: GoldBundle) {
                purchaseGoldBundle(goldBundle)
            }
        })
    }

    private lateinit var billingClient: BillingClient
    private val buyGoldOption by lazy { GoldBundlesFragmentArgs.fromBundle(arguments).buyGoldOption }

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
            processInAppPurchases(responseCode, purchases)
        })
        billingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
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

    private fun processInAppPurchases(responseCode: Int, purchases: MutableList<Purchase>?) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            launch(dispatchConfig.main) {
                viewModel.processInAppPurchases(purchases).observe(this@GoldBundlesFragment, Observer { purchaseStatuses ->
                    purchaseStatuses.filter { it.result is CaffeineResult.Success }
                            .forEach {
                                billingClient.consumeAsync(it.purchaseToken) { responseCode, purchaseToken ->
                                    if (responseCode == BillingClient.BillingResponse.OK) {
                                        Timber.d("Successfully consumed the purchase $purchaseToken")
                                    } else {
                                        Timber.e("Failed to consume the purchase $purchaseToken")
                                    }
                                }
                            }
                    purchaseStatuses.filter { it.result !is CaffeineResult.Success }
                            .forEach {
                                Timber.e("Failed to process in-app purchase ${it.purchaseToken}")
                            }
                })
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            Timber.d("User canceled purchase")
        } else {
            Timber.e("Failed to make a purchase $responseCode")
        }
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
                            for (sku in skuList) {
                                billingClient.queryPurchaseHistoryAsync(sku) { responseCode, purchasesList ->
                                    processInAppPurchases(responseCode, purchasesList)
                                }
                            }
                        }
                    }
                }
                else -> activity?.showSnackbar(R.string.error_loading_gold_bundles)
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
        val params = BillingFlowParams.newBuilder().setSku(sku).setType(BillingClient.SkuType.INAPP).build()
        billingClient.launchBillingFlow(activity, params)
    }

}

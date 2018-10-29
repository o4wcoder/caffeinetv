package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.android.billingclient.api.*
import timber.log.Timber
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.databinding.FragmentGoldBundlesBinding
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.ui.CaffeineFragment

class GoldBundlesFragment : CaffeineFragment() {

    private lateinit var binding: FragmentGoldBundlesBinding
    private val viewModel by lazy { viewModelProvider.get(GoldBundlesViewModel::class.java) }
    private val goldBundlesAdapter = GoldBundlesAdapter(object : GoldBundleClickListener {
        override fun onClick(goldBundle: GoldBundle) {
            purchaseGoldBundle(goldBundle)
        }
    })

    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val handler = Handler()
        viewModel.goldBundles.observe(viewLifecycleOwner, Observer { result ->
            handle(result, view) { paymentsEnvelope ->
                val list = paymentsEnvelope.payload.goldBundles.state.filter { it.usingStoreKit != null }
                val skuList = list.mapNotNull { it.usingStoreKit }.map { it.productId }
                val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(BillingClient.SkuType.INAPP).build()
                billingClient.querySkuDetailsAsync(params) { responseCode, skuDetailsList ->
                    Timber.d("Results: $skuDetailsList")
                    list.forEach {  goldBundle ->
                        goldBundle.skuDetails = skuDetailsList.find { it.sku == goldBundle.usingStoreKit?.productId }
                    }
                    handler.post { goldBundlesAdapter.submitList(list) }
                }
            }
        })
    }

    private fun purchaseGoldBundle(goldBundle: GoldBundle) {
        val activity = activity ?: return
        val sku = goldBundle.skuDetails?.sku ?: return
        val params = BillingFlowParams.newBuilder().setSku(sku).setType(BillingClient.SkuType.INAPP).build()
        billingClient.launchBillingFlow(activity, params)
    }

}

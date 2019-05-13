package tv.caffeine.app.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.util.BillingHelper
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.PurchaseOption
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.InjectionActivityTestRule
import tv.caffeine.app.util.TestDispatchConfig
import tv.caffeine.app.wallet.WalletRepository
import java.util.Date

class GoldBundlesViewModelTests {

    @RunWith(RobolectricTestRunner::class)
    class GooglePlayTests {
        private lateinit var subject: GoldBundlesViewModel
        private lateinit var billingClientBroadcastHelper: BillingClientBroadcastHelper
        @MockK private lateinit var processPlayStorePurchaseUseCase: ProcessPlayStorePurchaseUseCase
        private val activityTestRule = InjectionActivityTestRule(MainActivity::class.java, DaggerTestComponent.builder())
        private val mainActivity = activityTestRule.launchActivity(Intent())

        @Before
        fun setup() {
            MockKAnnotations.init(this)
            billingClientBroadcastHelper = BillingClientBroadcastHelper(mainActivity)
            val settingsStorage = InMemorySettingsStorage(caid = "random")
            val gson = Gson()
            val paymentsClientService = mockk<PaymentsClientService>(relaxed = true)
            val walletRepository = WalletRepository(TestDispatchConfig, gson, paymentsClientService)
            val loadGoldBundlesUseCase = LoadGoldBundlesUseCase(paymentsClientService, gson)
            val purchaseGoldBundleUseCase = PurchaseGoldBundleUseCase(paymentsClientService, gson)
            coEvery { processPlayStorePurchaseUseCase.invoke(any(), any()) } returns CaffeineResult.Success("random")
            subject = GoldBundlesViewModel(TestDispatchConfig, mainActivity.applicationContext, TokenStore(settingsStorage), walletRepository, loadGoldBundlesUseCase, purchaseGoldBundleUseCase, processPlayStorePurchaseUseCase)
        }

        @After
        fun cleanup() {
            subject.events.removeObservers(mainActivity)
        }

        @Test
        fun `attempting to purchase a bundle with invalid SKU report an error`() {
            val bundle = GoldBundle("a", 1, 1, null, null, PurchaseOption.PurchaseUsingInAppBilling("b", true), null, null)
            subject.purchaseGoldBundleUsingPlayStore(mainActivity, bundle)
            val observer: Observer<in Event<PurchaseStatus>> = Observer { event ->
                val purchaseStatus = event.getContentIfNotHandled()
                        ?: Assert.fail("Expected to have an unprocessed event")
                when (purchaseStatus) {
                    is PurchaseStatus.Error -> assertEquals("Expected to get the error_missing_sku", R.string.error_missing_sku, purchaseStatus.error)
                    else -> Assert.fail("Expected to receive the Error event, got $purchaseStatus instead")
                }
            }
            subject.events.observe(mainActivity, observer)
        }

        @Test
        fun `attempting to purchase a bundle with a valid SKU is successful`() {
            val skuDetails = SkuDetails("{\"productId\":\"1\"}")
            val bundle = GoldBundle("a", 1, 1, null, null, PurchaseOption.PurchaseUsingInAppBilling("b", true), null, skuDetails)
            subject.purchaseGoldBundleUsingPlayStore(mainActivity, bundle)

            billingClientBroadcastHelper.broadcastPurchaseSuccess(skuDetails)

            val observer: Observer<in Event<PurchaseStatus>> = Observer { event ->
                val purchaseStatus = event.getContentIfNotHandled() ?: Assert.fail("Expected to have an unprocessed event")
                when (purchaseStatus) {
                    is PurchaseStatus.GooglePlaySuccess -> assertNotNull("Expected success", purchaseStatus.purchaseToken)
                    is PurchaseStatus.Error -> Assert.fail("Expected GooglePlaySuccess, got error with message ${mainActivity.getString(purchaseStatus.error)}")
                    else -> Assert.fail("Expected to receive the GooglePlaySuccess event, got $purchaseStatus instead")
                }
            }
            subject.events.observe(mainActivity, observer)
        }

        @Test
        fun `user canceling the purchase flow is reported`() {
            val skuDetails = SkuDetails("{\"productId\":\"1\"}")
            val bundle = GoldBundle("a", 1, 1, null, null, PurchaseOption.PurchaseUsingInAppBilling("b", true), null, skuDetails)
            subject.purchaseGoldBundleUsingPlayStore(mainActivity, bundle)
            billingClientBroadcastHelper.broadcastUserCanceled()
            val observer: Observer<in Event<PurchaseStatus>> = Observer { event ->
                val purchaseStatus = event.getContentIfNotHandled() ?: Assert.fail("Expected to have an unprocessed event")
                when (purchaseStatus) {
                    is PurchaseStatus.Error -> Assert.fail("Expected CanceledByUser, got error with message ${mainActivity.getString(purchaseStatus.error)}")
                    PurchaseStatus.CanceledByUser -> Assert.assertTrue(true)
                    else -> Assert.fail("Expected to receive the CanceledByUser event, got $purchaseStatus instead")
                }
            }
            subject.events.observe(mainActivity, observer)
        }
    }

    class BillingClientBroadcastHelper(private val context: Context) {
        // helper code from BillingX

        companion object {
            private const val RESPONSE_INTENT_ACTION = "proxy_activity_response_intent_action"
            private const val RESPONSE_CODE = "response_code_key"
            private const val RESPONSE_BUNDLE = "response_bundle_key"
        }

        fun broadcastPurchaseSuccess(item: SkuDetails) {
            val skuType = BillingClient.SkuType.INAPP
            broadcastResult(BillingClient.BillingResponse.OK, buildResultBundle(item.toPurchaseData(context, skuType)))
        }

        fun broadcastUserCanceled() {
            broadcastResult(BillingClient.BillingResponse.USER_CANCELED, Bundle())
        }

        private fun SkuDetails.toPurchaseData(context: Context, @BillingClient.SkuType skuType: String): Purchase {
            val json = """{"orderId":"$sku..0","packageName":"${context.packageName}","productId":"$sku","autoRenewing":true,"purchaseTime":"${Date().time}","purchaseToken":"0987654321"}""".trimMargin()
            return Purchase(json, "debug-signature-$sku-$skuType")
        }

        private fun broadcastResult(responseCode: Int, resultBundle: Bundle) {
            val intent = Intent(RESPONSE_INTENT_ACTION)
            intent.putExtra(RESPONSE_CODE, responseCode)
            intent.putExtra(RESPONSE_BUNDLE, resultBundle)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        private fun buildResultBundle(purchase: Purchase): Bundle {
            return Bundle().apply {
                putInt(BillingHelper.RESPONSE_CODE, BillingClient.BillingResponse.OK)
                putStringArrayList(BillingHelper.RESPONSE_INAPP_PURCHASE_DATA_LIST, arrayListOf(purchase.originalJson))
                putStringArrayList(BillingHelper.RESPONSE_INAPP_SIGNATURE_LIST, arrayListOf(purchase.signature))
            }
        }
    }
}

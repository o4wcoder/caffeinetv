package tv.caffeine.app.settings

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PriceChangeConfirmationListener
import com.android.billingclient.api.PriceChangeFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.RewardLoadParams
import com.android.billingclient.api.RewardResponseListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.PurchaseOption
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.InjectionActivityTestRule
import tv.caffeine.app.test.observeForTesting
import tv.caffeine.app.util.CoroutinesTestRule
import tv.caffeine.app.util.TestDispatchConfig
import tv.caffeine.app.wallet.WalletRepository

class GoldBundlesViewModelTests {

    @RunWith(RobolectricTestRunner::class)
    class GooglePlayTests {
        @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
        @get:Rule val coroutinesTestRule = CoroutinesTestRule()

        private lateinit var subject: GoldBundlesViewModel
        @MockK private lateinit var processPlayStorePurchaseUseCase: ProcessPlayStorePurchaseUseCase
        @MockK private lateinit var billingClientFactory: BillingClientFactory
        private lateinit var billingClient: TestBillingClient
        private val activityTestRule = InjectionActivityTestRule(MainActivity::class.java, DaggerTestComponent.factory())
        private val mainActivity = activityTestRule.launchActivity(Intent())

        @Before
        fun setup() {
            MockKAnnotations.init(this)
            val settingsStorage = InMemorySettingsStorage(caid = "random")
            val secureSettingsStorage = InMemorySecureSettingsStorage()
            val gson = Gson()
            val paymentsClientService = mockk<PaymentsClientService>(relaxed = true)
            val walletRepository = WalletRepository(TestDispatchConfig, gson, paymentsClientService)
            val loadGoldBundlesUseCase = LoadGoldBundlesUseCase(paymentsClientService, gson)
            val purchaseGoldBundleUseCase = PurchaseGoldBundleUseCase(paymentsClientService, gson)
            coEvery { processPlayStorePurchaseUseCase.invoke(any(), any()) } returns CaffeineResult.Success("random")
            val slot = slot<PurchasesUpdatedListener>()
            billingClient = TestBillingClient()
            every { billingClientFactory.createBillingClient(any(), capture(slot)) } answers {
                billingClient.also {
                    it.purchasesUpdatedListener = slot.captured
                }
            }
            subject = GoldBundlesViewModel(
                mainActivity.applicationContext,
                billingClientFactory,
                TokenStore(settingsStorage, secureSettingsStorage),
                walletRepository,
                loadGoldBundlesUseCase,
                purchaseGoldBundleUseCase,
                processPlayStorePurchaseUseCase
            )
        }

        @Test
        fun `attempting to purchase a bundle with invalid SKU report an error`() {
            val bundle = GoldBundle("a", 1, 1, null, null, PurchaseOption.PurchaseUsingInAppBilling("b", true), null, null)
            billingClient.billingResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE).build()
            subject.purchaseGoldBundleUsingPlayStore(mainActivity, bundle)
            subject.events.observeForTesting { event ->
                val purchaseStatus = event.getContentIfNotHandled()
                        ?: Assert.fail("Expected to have an unprocessed event")
                when (purchaseStatus) {
                    is PurchaseStatus.Error -> assertEquals("Expected to get the error_missing_sku", R.string.error_missing_sku, purchaseStatus.error)
                    else -> Assert.fail("Expected to receive the Error event, got $purchaseStatus instead")
                }
            }
        }

        @Test
        fun `attempting to purchase a bundle with a valid SKU is successful`() = coroutinesTestRule.testDispatcher.runBlockingTest {
            val skuDetails = SkuDetails("{\"productId\":\"1\"}")
            val bundle = GoldBundle("a", 1, 1, null, null, PurchaseOption.PurchaseUsingInAppBilling("b", true), null, skuDetails)
            billingClient.billingResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            billingClient.purchases = listOf(mockk(relaxed = true))
            billingClient.consumeBillingResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            billingClient.purchaseToken = "aha"
            subject.purchaseGoldBundleUsingPlayStore(mainActivity, bundle)

            subject.events.observeForTesting { event ->
                val purchaseStatus = event.getContentIfNotHandled() ?: Assert.fail("Expected to have an unprocessed event")
                when (purchaseStatus) {
                    is PurchaseStatus.GooglePlaySuccess -> assertNotNull("Expected success", purchaseStatus.purchaseToken)
                    is PurchaseStatus.Error -> Assert.fail("Expected GooglePlaySuccess, got error with message ${mainActivity.getString(purchaseStatus.error)}")
                    else -> Assert.fail("Expected to receive the GooglePlaySuccess event, got $purchaseStatus instead")
                }
            }
        }

        @Test
        fun `user canceling the purchase flow is reported`() {
            val skuDetails = SkuDetails("{\"productId\":\"1\"}")
            val bundle = GoldBundle("a", 1, 1, null, null, PurchaseOption.PurchaseUsingInAppBilling("b", true), null, skuDetails)
            billingClient.billingResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED).build()
            subject.purchaseGoldBundleUsingPlayStore(mainActivity, bundle)
            subject.events.observeForTesting { event ->
                val purchaseStatus = event.getContentIfNotHandled() ?: Assert.fail("Expected to have an unprocessed event")
                when (purchaseStatus) {
                    is PurchaseStatus.Error -> Assert.fail("Expected CanceledByUser, got error with message ${mainActivity.getString(purchaseStatus.error)}")
                    PurchaseStatus.CanceledByUser -> assertTrue(true)
                    else -> Assert.fail("Expected to receive the CanceledByUser event, got $purchaseStatus instead")
                }
            }
        }

        @Test
        fun `gold bundles buyable with credits must be buyable with in app billing`() {
            val allGoldBundles = listOf(
                GoldBundle("1", 1, 1, mockk(), null, mockk(), null, null),
                GoldBundle("2", 2, 2, mockk(), null, null, null, null)
            )
            val filteredGoldBundles = subject.getGoldBundlesUsingCredits(allGoldBundles)
            assertEquals(1, filteredGoldBundles.size)
            assertEquals("1", filteredGoldBundles[0].id)
        }

        @Test
        fun `gold bundles buyable with in app billing should have the in app billing purchase option`() {
            val allGoldBundles = listOf(
                GoldBundle("1", 1, 1, mockk(), null, mockk(), null, null),
                GoldBundle("2", 2, 2, mockk(), null, null, null, null))
            val filteredGoldBundles = subject.getGoldBundlesUsingPlayStore(allGoldBundles)
            assertEquals(1, filteredGoldBundles.size)
            assertEquals("1", filteredGoldBundles[0].id)
        }
    }

    class TestBillingClient: BillingClient() {
        var purchasesUpdatedListener: PurchasesUpdatedListener? = null
        var billingResult: BillingResult? = null
        var purchases: List<Purchase>? = null
        var consumeBillingResult: BillingResult? = null
        var purchaseToken: String? = null

        override fun isFeatureSupported(feature: String?): BillingResult {
            return BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build()
        }

        override fun endConnection() {
        }

        override fun launchPriceChangeConfirmationFlow(
            activity: Activity?,
            params: PriceChangeFlowParams?,
            listener: PriceChangeConfirmationListener
        ) {
        }

        override fun startConnection(listener: BillingClientStateListener) {
        }

        override fun consumeAsync(
            consumeParams: ConsumeParams?,
            listener: ConsumeResponseListener
        ) {
            listener.onConsumeResponse(consumeBillingResult, purchaseToken)
        }

        override fun queryPurchaseHistoryAsync(
            skuType: String?,
            listener: PurchaseHistoryResponseListener
        ) {
        }

        override fun launchBillingFlow(
            activity: Activity?,
            params: BillingFlowParams?
        ): BillingResult {
            purchasesUpdatedListener?.let {
                it.onPurchasesUpdated(billingResult, purchases)
            }
            return BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build()
        }

        override fun querySkuDetailsAsync(
            params: SkuDetailsParams?,
            listener: SkuDetailsResponseListener
        ) {
        }

        override fun isReady() = true

        override fun loadRewardedSku(params: RewardLoadParams?, listener: RewardResponseListener) {
        }

        override fun acknowledgePurchase(
            params: AcknowledgePurchaseParams?,
            listener: AcknowledgePurchaseResponseListener?
        ) {
        }

        override fun queryPurchases(skuType: String?): Purchase.PurchasesResult {
            return Purchase.PurchasesResult(
                BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build(),
                listOf()
            )
        }
    }
}

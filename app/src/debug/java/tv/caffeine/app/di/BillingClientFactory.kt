package tv.caffeine.app.di

import android.content.Context
import androidx.arch.core.executor.ArchTaskExecutor
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.pixite.android.billingx.BillingStore
import com.pixite.android.billingx.DebugBillingClient
import com.pixite.android.billingx.SkuDetailsBuilder
import java.util.concurrent.Executor

object BillingClientFactory {

    fun createBillingClient(context: Context, listener: PurchasesUpdatedListener) =
            DebugBillingClient(context, listener, Executor {
                    ArchTaskExecutor.getInstance().postToMainThread(it)
            })

    fun loadBillingStore(context: Context) {
        BillingStore.defaultStore(context)
                .clearProducts()
                .addProduct(
                        SkuDetailsBuilder("gold.bundle.v2.buy10", BillingClient.SkuType.INAPP, "0.99",
                                990000L, "USD", "10 Gold Bundle", "10 Gold Bundle").build()
                )
                .addProduct(
                        SkuDetailsBuilder("gold.bundle.v2.buy50", BillingClient.SkuType.INAPP, "4.99",
                                4990000L, "USD", "50 Gold Bundle", "50 Gold Bundle").build()
                )
                .addProduct(
                        SkuDetailsBuilder("gold.bundle.v2.buy100", BillingClient.SkuType.INAPP, "9.99",
                                9990000L, "USD", "100 Gold Bundle", "100 Gold Bundle").build()
                )
                .addProduct(
                        SkuDetailsBuilder("gold.bundle.v2.buy255", BillingClient.SkuType.INAPP, "24.99",
                                24990000L, "USD", "255 Gold Bundle", "255 Gold Bundle").build()
                )
                .addProduct(
                        SkuDetailsBuilder("gold.bundle.v2.buy505", BillingClient.SkuType.INAPP, "49.99",
                                49990000L, "USD", "505 Gold Bundle", "505 Gold Bundle").build()
                )
                .addProduct(
                        SkuDetailsBuilder("gold.bundle.v2.buy1010", BillingClient.SkuType.INAPP, "99.99",
                                99990000L, "USD", "1010 Gold Bundle", "1010 Gold Bundle").build()
                )
    }
}

package tv.caffeine.app.di

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import javax.inject.Inject

class BillingClientFactory @Inject constructor() {
    fun createBillingClient(context: Context, listener: PurchasesUpdatedListener): BillingClient =
        BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases()
            .build()
}

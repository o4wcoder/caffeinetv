package tv.caffeine.app.di

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

object BillingClientFactory {
    fun createBillingClient(context: Context, listener: PurchasesUpdatedListener) =
            BillingClient.newBuilder(context)
                    .setListener(listener)
                    .build()

    fun loadBillingStore(context: Context) {
        // Intentionally empty
    }
}

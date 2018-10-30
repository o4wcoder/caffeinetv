package tv.caffeine.app.di

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

object BillingClientFactory {
    fun createBillingClient(activity: Activity, listener: PurchasesUpdatedListener) =
            BillingClient.newBuilder(activity)
                    .setListener(listener)
                    .build()

    fun loadBillingStore(context: Context) {
        // Intentionally empty
    }
}

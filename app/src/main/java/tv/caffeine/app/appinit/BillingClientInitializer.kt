package tv.caffeine.app.appinit

import android.app.Application
import tv.caffeine.app.di.BillingClientFactory
import javax.inject.Inject

class BillingClientInitializer @Inject constructor(
) : AppInitializer {
    override fun init(application: Application) {
        BillingClientFactory.loadBillingStore(application)
    }
}

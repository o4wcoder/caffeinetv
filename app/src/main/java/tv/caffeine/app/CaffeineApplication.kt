package tv.caffeine.app

import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import timber.log.Timber
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.di.DaggerCaffeineComponent
import javax.inject.Inject

class CaffeineApplication : DaggerApplication() {
    @Inject lateinit var timberTree: Timber.Tree
    @Inject lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        Timber.plant(timberTree)
        BillingClientFactory.loadBillingStore(this)
        analytics.initialize()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication>
            = DaggerCaffeineComponent.builder().create(this)
}

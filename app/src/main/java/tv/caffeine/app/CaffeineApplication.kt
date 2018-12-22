package tv.caffeine.app

import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import timber.log.Timber
import tv.caffeine.app.di.BillingClientFactory
import tv.caffeine.app.di.DaggerCaffeineComponent
import javax.inject.Inject

class CaffeineApplication : DaggerApplication() {
    @Inject lateinit var timberTree: Timber.Tree

    override fun onCreate() {
        super.onCreate()
        // This process is dedicated to LeakCanary for heap analysis.
        // You should not init your app in this process.
        if (LeakCanary.isInAnalyzerProcess(this)) return
        LeakCanary.install(this)
        AndroidThreeTen.init(this)
        Timber.plant(timberTree)
        BillingClientFactory.loadBillingStore(this)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication>
            = DaggerCaffeineComponent.builder().create(this)
}

package tv.caffeine.app

import com.squareup.leakcanary.LeakCanary
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import timber.log.Timber
import tv.caffeine.app.di.DaggerCaffeineComponent
import tv.caffeine.app.util.CrashlyticsTree

class CaffeineApplication : DaggerApplication() {
    override fun onCreate() {
        super.onCreate()
        // This process is dedicated to LeakCanary for heap analysis.
        // You should not init your app in this process.
        if (LeakCanary.isInAnalyzerProcess(this)) return
        LeakCanary.install(this)
        initializeTimber()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication>
            = DaggerCaffeineComponent.builder().create(this)

    private fun initializeTimber() {
        val tree = if (BuildConfig.DEBUG) CrashlyticsTree() else Timber.DebugTree()
        Timber.plant(tree)
    }
}
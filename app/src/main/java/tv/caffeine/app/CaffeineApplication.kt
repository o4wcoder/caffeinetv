package tv.caffeine.app

import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import timber.log.Timber
import tv.caffeine.app.di.DaggerCaffeineComponent

class CaffeineApplication : DaggerApplication() {
    override fun onCreate() {
        super.onCreate()
        initializeTimber()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication>
            = DaggerCaffeineComponent.builder().create(this)

    private fun initializeTimber() {
        Timber.plant(Timber.DebugTree())
    }
}
package tv.caffeine.app

import com.squareup.picasso.Picasso
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import tv.caffeine.app.appinit.AppInitializers
import tv.caffeine.app.di.DaggerCaffeineComponent
import javax.inject.Inject
import javax.inject.Provider

class CaffeineApplication : DaggerApplication() {
    lateinit var injector: AndroidInjector<out DaggerApplication>

    @Inject lateinit var initializers: AppInitializers
    @Inject lateinit var picassoProvider: Provider<Picasso>

    override fun onCreate() {
        injector = DaggerCaffeineComponent.builder().create(this)
        super.onCreate()
        initializers.init(this)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> = injector
}

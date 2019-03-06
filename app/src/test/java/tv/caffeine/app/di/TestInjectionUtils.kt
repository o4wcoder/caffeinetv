package tv.caffeine.app.di

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ActivityTestRule
import com.google.firebase.FirebaseApp
import dagger.android.AndroidInjector
import tv.caffeine.app.CaffeineApplication

class InjectionActivityTestRule<T : Activity>(
        activityClass: Class<T>,
        private val componentBuilder: AndroidInjector.Builder<CaffeineApplication>
) : ActivityTestRule<T>(activityClass, true, false) {
    override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = componentBuilder.create(app)
        app.setApplicationInjector(testComponent)
        FirebaseApp.initializeApp(app)
    }
}

fun CaffeineApplication.setApplicationInjector(injector: AndroidInjector<CaffeineApplication>) {
    this.injector = injector.also {
        it.inject(this)
    }
}

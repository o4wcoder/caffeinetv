package tv.caffeine.app.di

import android.app.Application
import dagger.Binds
import dagger.Module
import tv.caffeine.app.CaffeineApplication

const val CAFFEINE_SHARED_PREFERENCES = "caffeine"
const val REFRESH_TOKEN = "REFRESH_TOKEN"

@Module
abstract class AppModule {
    @Binds
    abstract fun application(app: CaffeineApplication): Application
}
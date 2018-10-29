package tv.caffeine.app.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.notifications.NotificationAuthWatcher
import javax.inject.Named
import javax.inject.Singleton

const val REFRESH_TOKEN = "REFRESH_TOKEN"
private const val CAFFEINE_SHARED_PREFERENCES = "caffeine"

@Module
class DataModule {

    @Provides
    fun providesContext(application: Application): Context = application

    @Provides
    fun providesCaffeineSharedPreferences(context: Context) = context.getSharedPreferences(CAFFEINE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun providesTokenStore(sharedPreferences: SharedPreferences) = TokenStore(sharedPreferences)

    @Provides
    @Singleton
    fun providesAuthWatcher(notificationAuthWatcher: NotificationAuthWatcher): AuthWatcher = notificationAuthWatcher

    @Provides
    @Named(REFRESH_TOKEN)
    fun providesRefreshToken(sharedPreferences: SharedPreferences): String? = sharedPreferences.getString(REFRESH_TOKEN, null)

}

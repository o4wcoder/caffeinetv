package tv.caffeine.app.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Named

const val CAFFEINE_SHARED_PREFERENCES = "caffeine"
const val REFRESH_TOKEN = "REFRESH_TOKEN"

@Module
class AppModule {
    @Provides
    fun providesCaffeineSharedPreferences(context: Context) = context.getSharedPreferences(CAFFEINE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

    @Provides
    @Named(REFRESH_TOKEN)
    fun providesRefreshToken(sharedPreferences: SharedPreferences): String? = sharedPreferences.getString(REFRESH_TOKEN, null)
}
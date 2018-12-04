package tv.caffeine.app.di

import dagger.Module
import dagger.Provides

const val ASSETS_BASE_URL = "https://assets.caffeine.tv"
const val IMAGES_BASE_URL = "https://images.caffeine.tv"
const val REALTIME_WEBSOCKET_URL = "wss://realtime.caffeine.tv"

@Module
class ServerConfigModule {

    @Provides
    @CaffeineApi(Service.MainApi)
    fun providesBaseUrl() = "https://api.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Realtime)
    fun providesRealtimeBaseUrl() = "https://realtime.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Payments)
    fun providesPaymentsBaseUrl() = "https://payments.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Events)
    fun providesEventsBaseUrl() = "https://events.caffeine.tv"
}

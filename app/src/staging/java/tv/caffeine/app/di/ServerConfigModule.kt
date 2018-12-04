package tv.caffeine.app.di

import dagger.Module
import dagger.Provides

const val ASSETS_BASE_URL = "https://assets.staging.caffeine.tv"
const val IMAGES_BASE_URL = "https://images.staging.caffeine.tv"
const val REALTIME_WEBSOCKET_URL = "wss://realtime.staging.caffeine.tv"

@Module
class ServerConfigModule {

    @Provides
    @CaffeineApi(Service.MainApi)
    fun providesBaseUrl() = "https://api.staging.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Realtime)
    fun providesRealtimeBaseUrl() = "https://realtime.staging.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Payments)
    fun providesPaymentsBaseUrl() = "https://payments.staging.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Events)
    fun providesEventsBaseUrl() = "https://events.staging.caffeine.tv"
}

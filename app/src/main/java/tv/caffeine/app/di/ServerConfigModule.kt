package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import tv.caffeine.app.net.ServerConfig

const val ASSETS_BASE_URL = "https://assets.caffeine.tv"
const val IMAGES_BASE_URL = "https://images.caffeine.tv"

@Module
class ServerConfigModule {

    @Provides
    @CaffeineApi(Service.MainApi)
    fun providesBaseUrl(serverConfig: ServerConfig) = serverConfig.api

    @Provides
    @CaffeineApi(Service.Realtime)
    fun providesRealtimeBaseUrl(serverConfig: ServerConfig) = serverConfig.realtime

    @Provides
    @CaffeineApi(Service.Payments)
    fun providesPaymentsBaseUrl(serverConfig: ServerConfig) = serverConfig.payments

    @Provides
    @CaffeineApi(Service.Events)
    fun providesEventsBaseUrl(serverConfig: ServerConfig) = serverConfig.events

    @Provides
    @CaffeineApi(Service.ContentGuide)
    fun providesContentGuideBaseUrl(serverConfig: ServerConfig) = serverConfig.contentGuide
}

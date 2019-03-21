package tv.caffeine.app.di

import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import io.mockk.mockk
import tv.caffeine.app.api.*
import javax.inject.Singleton

@Module(includes = [
    GsonModule::class,
    OkHttpModule::class,
    FakeApiModule::class,
    WebRtcModule::class,
    FakeImageLoadingModule::class,
    ServerConfigModule::class
])
class TestNetworkModule

@Module
class FakeApiModule {
    @Provides fun providesAccountsService() = mockk<AccountsService>(relaxed = true)
    @Provides fun providesLobbyService() = mockk<LobbyService>(relaxed = true)
    @Provides fun providesEventsService() = mockk<EventsService>(relaxed = true)
    @Provides fun providesUsersService() = mockk<UsersService>(relaxed = true)
    @Provides fun providesSearchService() = mockk<SearchService>(relaxed = true)
    @Provides fun providesBroadcastsService() = mockk<BroadcastsService>(relaxed = true)
    @Provides fun providesDevicesService() = mockk<DevicesService>(relaxed = true)
    @Provides fun providesPaymentsClientService() = mockk<PaymentsClientService>(relaxed = true)
    @Provides fun providesOAuthService() = mockk<OAuthService>(relaxed = true)
    @Provides fun providesVersionCheckService() = mockk<VersionCheckService>(relaxed = true)
    @Provides fun providesRefreshTokenService() = mockk<RefreshTokenService>(relaxed = true)
    @Provides fun providesRealtimeService() = mockk<Realtime>(relaxed = true)
    @Provides fun providesFeatureConfigService() = mockk<FeatureConfigService>(relaxed = true)
}

@Module
class FakeImageLoadingModule {
    @Provides
    @Singleton
    fun providesPicasso(): Picasso = mockk<Picasso>(relaxed = true)
}

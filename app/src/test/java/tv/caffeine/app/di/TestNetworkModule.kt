package tv.caffeine.app.di

import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import io.mockk.mockk
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.DevicesService
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.FeatureConfigService
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.OAuthService
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.RefreshTokenService
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.VersionCheckService
import tv.caffeine.app.webrtc.SurfaceViewRendererTuner
import javax.inject.Singleton

@Module(includes = [
    GsonModule::class,
    OkHttpModule::class,
    FakeApiModule::class,
    FakeWebRtcModule::class,
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
class FakeWebRtcModule {
    @Provides
    @Singleton
    fun providesEglBase(): EglBase = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providesSurfaceViewRendererTuner(eglBase: EglBase): SurfaceViewRendererTuner = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providesVideoEncoderFactory(): DefaultVideoEncoderFactory = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providesVideoDecoderFactory(): DefaultVideoDecoderFactory = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providesPeerConnectionFactory(): PeerConnectionFactory = mockk(relaxed = true)
}

@Module
class FakeImageLoadingModule {
    @Provides
    @Singleton
    fun providesPicasso(): Picasso = mockk(relaxed = true)
}

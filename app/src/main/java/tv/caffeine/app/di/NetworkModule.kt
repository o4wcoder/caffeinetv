package tv.caffeine.app.di

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.createEglBase14
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.caffeine.app.api.*
import tv.caffeine.app.auth.AuthorizationInterceptor
import tv.caffeine.app.auth.TokenAuthenticator
import tv.caffeine.app.auth.TokenStore
import javax.inject.Singleton

enum class Service {
    MainApi, RefreshToken, Payments, Realtime
}

@Module
class NetworkModule {

    @Provides
    fun providesGson(): Gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    @Provides
    fun providesGsonConverterFactory(gson: Gson): GsonConverterFactory = GsonConverterFactory.create(gson)

    @Provides
    fun providesHttpLoggingInterceptor(level: HttpLoggingInterceptor.Level) = HttpLoggingInterceptor().apply { setLevel(level) }

    @Provides
    fun providesRefreshTokenService(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.MainApi) baseUrl: String, loggingInterceptor: HttpLoggingInterceptor): RefreshTokenService {
        val okHttpClient = OkHttpClient.Builder().addNetworkInterceptor(loggingInterceptor).build()
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(gsonConverterFactory)
                .build()
        return retrofit.create(RefreshTokenService::class.java)
    }

    @Provides
    fun providesTokenAuthenticator(refreshTokenService: RefreshTokenService, tokenStore: TokenStore) = TokenAuthenticator(refreshTokenService, tokenStore)

    @Provides
    fun providesAuthorizationInterceptor(tokenStore: TokenStore) = AuthorizationInterceptor(tokenStore)

    @Provides
    fun providesOkHttpClient(tokenAuthenticator: TokenAuthenticator, authorizationInterceptor: AuthorizationInterceptor, loggingInterceptor: HttpLoggingInterceptor) = OkHttpClient.Builder()
            .authenticator(tokenAuthenticator)
            .addInterceptor(authorizationInterceptor)
            .addNetworkInterceptor(loggingInterceptor)
            .build()

    @Provides
    @CaffeineApi(Service.MainApi)
    fun providesRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.MainApi) baseUrl: String, client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides
    @CaffeineApi(Service.Payments)
    fun providesPaymentsRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.Payments) baseUrl: String, client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides
    @CaffeineApi(Service.MainApi)
    fun providesBaseUrl() = "https://api.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Realtime)
    fun providesRealtimeBaseUrl() = "https://realtime.caffeine.tv"

    @Provides
    @CaffeineApi(Service.Payments)
    fun providesPaymentsBaseUrl() = "https://payments.caffeine.tv"

    @Provides fun providesAccountsService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(AccountsService::class.java)

    @Provides fun providesLobbyService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(LobbyService::class.java)

    @Provides fun providesEventsService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(EventsService::class.java)

    @Provides fun providesUsersService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(UsersService::class.java)

    @Provides fun providesSearchService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(SearchService::class.java)

    @Provides fun providesBroadcastsService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(BroadcastsService::class.java)

    @Provides fun providesPaymentsClientService(@CaffeineApi(Service.Payments) retrofit: Retrofit) = retrofit.create(PaymentsClientService::class.java)

    @Provides
    @CaffeineApi(Service.Realtime)
    fun providesRealtimeRetrofit(client: OkHttpClient, @CaffeineApi(Service.Realtime) baseUrl: String) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides fun providesRealtimeService(@CaffeineApi(Service.Realtime) retrofit: Retrofit) = retrofit.create(Realtime::class.java)

    @Provides
    @Singleton
    fun providesEglBase() = createEglBase14()

    @Provides
    @Singleton
    fun providesPeerConnectionFactory(context: Context, eglBase: EglBase): PeerConnectionFactory {
        val initializationOptions = PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
        val options = PeerConnectionFactory.Options()
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoDecoderFactory(videoDecoderFactory)
                .createPeerConnectionFactory()
    }
}
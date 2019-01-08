package tv.caffeine.app.di

import android.content.Context
import com.google.gson.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.webrtc.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.caffeine.app.api.*
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.net.AppMetaDataInterceptor
import tv.caffeine.app.net.AuthorizationInterceptor
import tv.caffeine.app.net.LongPollInterceptor
import tv.caffeine.app.net.TokenAuthenticator
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

enum class Service {
    MainApi, RefreshToken, Payments, Realtime, Events, RealtimeWebSocket
}

@Module
class NetworkModule {

    @Provides
    fun providesZonedDateTimeConverter(): JsonDeserializer<ZonedDateTime> = object : JsonDeserializer<ZonedDateTime> {
        private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ZonedDateTime {
            return formatter.parse(json?.asString, ZonedDateTime.FROM)
        }

    }

    @Provides
    fun providesGson(jsonDeserializer: JsonDeserializer<ZonedDateTime>): Gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(ZonedDateTime::class.java, jsonDeserializer)
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
    fun providesOkHttpClient(
            tokenAuthenticator: TokenAuthenticator,
            longPollInterceptor: LongPollInterceptor,
            appMetaDataInterceptor: AppMetaDataInterceptor,
            authorizationInterceptor: AuthorizationInterceptor,
            loggingInterceptor: HttpLoggingInterceptor
    ) = OkHttpClient.Builder()
            .authenticator(tokenAuthenticator)
            .addInterceptor(longPollInterceptor)
            .addInterceptor(appMetaDataInterceptor)
            .addInterceptor(authorizationInterceptor)
            .addNetworkInterceptor(loggingInterceptor)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
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
    @CaffeineApi(Service.Events)
    fun providesEventsRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.Events) baseUrl: String, client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides fun providesAccountsService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(AccountsService::class.java)

    @Provides fun providesLobbyService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(LobbyService::class.java)

    @Provides fun providesEventsService(@CaffeineApi(Service.Events) retrofit: Retrofit) = retrofit.create(EventsService::class.java)

    @Provides fun providesUsersService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(UsersService::class.java)

    @Provides fun providesSearchService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(SearchService::class.java)

    @Provides fun providesBroadcastsService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(BroadcastsService::class.java)

    @Provides fun providesDevicesService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(DevicesService::class.java)

    @Provides fun providesPaymentsClientService(@CaffeineApi(Service.Payments) retrofit: Retrofit) = retrofit.create(PaymentsClientService::class.java)

    @Provides fun providesOAuthService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(OAuthService::class.java)

    @Provides fun providesVersionCheckService(@CaffeineApi(Service.MainApi) retrofit: Retrofit) = retrofit.create(VersionCheckService::class.java)

    @Provides
    @CaffeineApi(Service.Realtime)
    fun providesRealtimeRetrofit(client: OkHttpClient, @CaffeineApi(Service.Realtime) baseUrl: String) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
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
        val videoEncoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .createPeerConnectionFactory()
    }
}

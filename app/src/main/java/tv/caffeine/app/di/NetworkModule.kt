package tv.caffeine.app.di

import android.content.Context
import com.google.gson.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import dagger.Module
import dagger.Provides
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
import javax.inject.Singleton

enum class Service {
    MainApi, RefreshToken, Payments, Realtime, Events, RealtimeWebSocket
}

enum class AuthorizationType {
    Required, NoAuthorization
}

@Module(includes = [GsonModule::class, OkHttpModule::class, RetrofitModule::class, ApiModule::class, WebRtcModule::class, ServerConfigModule::class])
class NetworkModule

@Module
class GsonModule {
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
}

@Module
class OkHttpModule {
    @Provides
    fun providesHttpLoggingInterceptor(level: HttpLoggingInterceptor.Level) = HttpLoggingInterceptor().apply { setLevel(level) }

    @Provides
    fun providesTokenAuthenticator(refreshTokenService: RefreshTokenService, tokenStore: TokenStore) = TokenAuthenticator(refreshTokenService, tokenStore)

    @Provides
    @ClientType(AuthorizationType.NoAuthorization)
    fun providesOkHttpClientWithoutAuthorization(
            longPollInterceptor: LongPollInterceptor,
            appMetaDataInterceptor: AppMetaDataInterceptor,
            loggingInterceptor: HttpLoggingInterceptor
    ) = OkHttpClient.Builder()
            .addInterceptor(longPollInterceptor)
            .addInterceptor(appMetaDataInterceptor)
            .addNetworkInterceptor(loggingInterceptor)
            .build()

    @Provides
    @ClientType(AuthorizationType.Required)
    fun providesOkHttpClientAuthorizationRequired(
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
            .build()

}

@Module
class RetrofitModule {
    @Provides
    @CaffeineApi(Service.MainApi)
    fun providesRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.MainApi) baseUrl: String, @ClientType(AuthorizationType.Required) client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides
    @CaffeineApi(Service.Payments)
    fun providesPaymentsRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.Payments) baseUrl: String, @ClientType(AuthorizationType.Required) client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides
    @CaffeineApi(Service.Events)
    fun providesEventsRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.Events) baseUrl: String, @ClientType(AuthorizationType.Required) client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides
    @CaffeineApi(Service.RefreshToken)
    fun providesRefreshTokenRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.MainApi) baseUrl: String, @ClientType(AuthorizationType.NoAuthorization) client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides
    @CaffeineApi(Service.Realtime)
    fun providesRealtimeRetrofit(gsonConverterFactory: GsonConverterFactory, @CaffeineApi(Service.Realtime) baseUrl: String, @ClientType(AuthorizationType.Required) client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
}

@Module
class ApiModule {
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

    @Provides fun providesRefreshTokenService(@CaffeineApi(Service.RefreshToken) retrofit: Retrofit) = retrofit.create(RefreshTokenService::class.java)

    @Provides fun providesRealtimeService(@CaffeineApi(Service.Realtime) retrofit: Retrofit) = retrofit.create(Realtime::class.java)
}

@Module
class WebRtcModule {
    @Provides
    @Singleton
    fun providesEglBase() = createEglBase14()

    @Provides
    @Singleton
    fun providesPeerConnectionFactory(context: Context, eglBase: EglBase, webRtcLoggable: Loggable?, webRtcLogLevel: Logging.Severity): PeerConnectionFactory {
        val initializationOptions = PeerConnectionFactory.InitializationOptions
                .builder(context)
                .setInjectableLogger(webRtcLoggable, webRtcLogLevel)
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

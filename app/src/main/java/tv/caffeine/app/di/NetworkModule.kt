package tv.caffeine.app.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.auth.AuthorizationInterceptor
import tv.caffeine.app.api.RefreshTokenService
import tv.caffeine.app.auth.TokenAuthenticator
import tv.caffeine.app.auth.TokenStore
import javax.inject.Named
import javax.inject.Singleton

const val BASE_URL = "BASE_URL"

@Module
class NetworkModule {
    @Provides
    fun providesContext(application: Application): Context = application

    @Provides
    fun providesCaffeineSharedPreferences(context: Context) = context.getSharedPreferences(CAFFEINE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

    @Provides
    @Named(REFRESH_TOKEN)
    fun providesRefreshToken(sharedPreferences: SharedPreferences): String? = sharedPreferences.getString(REFRESH_TOKEN, null)

    @Provides
    fun providesGson(): Gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    @Provides
    fun providesGsonConverterFactory(gson: Gson): GsonConverterFactory = GsonConverterFactory.create(gson)

    @Provides
    fun providesHttpLoggingLevel() = HttpLoggingInterceptor.Level.BODY

    @Provides
    fun providesHttpLoggingInterceptor(level: HttpLoggingInterceptor.Level) = HttpLoggingInterceptor().apply { setLevel(level) }

    @Provides
    @Singleton
    fun providesTokenStore(sharedPreferences: SharedPreferences) = TokenStore(sharedPreferences)

    @Provides
    fun providesRefreshTokenService(gsonConverterFactory: GsonConverterFactory, @Named(BASE_URL) baseUrl: String): RefreshTokenService {
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
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
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    fun providesRetrofit(gsonConverterFactory: GsonConverterFactory, @Named(BASE_URL) baseUrl: String, client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Provides
    @Named(BASE_URL)
    fun providesBaseUrl() = "https://api.caffeine.tv"

    @Provides fun providesAccountsService(retrofit: Retrofit) = retrofit.create(AccountsService::class.java)

    @Provides fun providesLobbyService(retrofit: Retrofit) = retrofit.create(LobbyService::class.java)

    @Provides fun providesEventsService(retrofit: Retrofit) = retrofit.create(EventsService::class.java)

    @Provides fun providesRealtimeService(client: OkHttpClient): Realtime {
//        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        val retrofit = Retrofit.Builder()
                .baseUrl("https://realtime.caffeine.tv")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
//                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        return retrofit.create(Realtime::class.java)
    }

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
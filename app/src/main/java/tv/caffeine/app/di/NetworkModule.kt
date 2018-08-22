package tv.caffeine.app.di

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.caffeine.app.auth.Accounts
import tv.caffeine.app.lobby.Lobby
import tv.caffeine.app.net.AuthorizationInterceptor
import javax.inject.Named

const val BASE_URL = "BASE_URL"

@Module
class NetworkModule {
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
    fun providesOkHttpClient(loggingInterceptor: HttpLoggingInterceptor) = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor())
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    fun providesRetrofit(gsonConverterFactory: GsonConverterFactory, @Named(BASE_URL) baseUrl: String, client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(gsonConverterFactory)
            .build()

    @Provides
    @Named(BASE_URL)
    fun providesBaseUrl() = "https://api.caffeine.tv"

    @Provides fun providesAccountsService(retrofit: Retrofit) = retrofit.create(Accounts::class.java)

    @Provides fun providesLobbyService(retrofit: Retrofit) = retrofit.create(Lobby::class.java)
}
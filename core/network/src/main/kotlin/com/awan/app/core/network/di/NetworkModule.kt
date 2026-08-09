package com.awan.app.core.network.di

import android.content.Context
import com.awan.app.core.network.BuildConfig
import com.awan.app.core.network.api.AuthApiService
import com.awan.app.core.network.api.CategoryApiService
import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.api.OnboardingApiService
import com.awan.app.core.network.api.GamificationApiService
import com.awan.app.core.network.api.ProfileApiService
import com.awan.app.core.network.api.StoreApiService
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.api.ZoneApiService
import com.awan.app.core.network.api.TemplateApiService
import com.awan.app.core.network.device.AndroidDeviceIdProvider
import com.awan.app.core.network.device.DeviceIdProvider
import com.awan.app.core.network.interceptor.AiTimeoutInterceptor
import com.awan.app.core.network.interceptor.AuthInterceptor
import com.awan.app.core.network.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuthOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L
    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = true
        encodeDefaults = true
        coerceInputValues = true
    }
    @Provides
    @Singleton
    fun providesLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    @NoAuthOkHttpClient
    fun providesNoAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AiTimeoutInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun providesOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(AiTimeoutInterceptor())
        .addInterceptor(loggingInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @NoAuthRetrofit
    fun providesNoAuthRetrofit(
        @NoAuthOkHttpClient okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.AWAN_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.AWAN_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesAuthApiService(@NoAuthRetrofit retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun providesOnboardingApiService(retrofit: Retrofit): OnboardingApiService =
        retrofit.create(OnboardingApiService::class.java)

    @Provides
    @Singleton
    fun providesProfileApiService(retrofit: Retrofit): ProfileApiService =
        retrofit.create(ProfileApiService::class.java)

    @Provides
    @Singleton
    fun providesStoreApiService(retrofit: Retrofit): StoreApiService =
        retrofit.create(StoreApiService::class.java)

    @Provides
    @Singleton
    fun providesTaskApiService(retrofit: Retrofit): TaskApiService =
        retrofit.create(TaskApiService::class.java)

    @Provides
    @Singleton
    fun providesZoneApiService(retrofit: Retrofit): ZoneApiService =
        retrofit.create(ZoneApiService::class.java)

    @Provides
    @Singleton
    fun providesCategoryApiService(retrofit: Retrofit): CategoryApiService =
        retrofit.create(CategoryApiService::class.java)

    @Provides
    @Singleton
    fun providesUserApiService(retrofit: Retrofit): com.awan.app.core.network.api.UserApiService =
        retrofit.create(com.awan.app.core.network.api.UserApiService::class.java)

    @Provides
    @Singleton
    fun providesSessionApiService(retrofit: Retrofit): com.awan.app.core.network.api.SessionApiService =
        retrofit.create(com.awan.app.core.network.api.SessionApiService::class.java)

    @Provides
    @Singleton
    fun providesGamificationApiService(retrofit: Retrofit): GamificationApiService =
        retrofit.create(GamificationApiService::class.java)

    @Provides
    @Singleton
    fun providesTemplateApiService(retrofit: Retrofit): TemplateApiService =
        retrofit.create(TemplateApiService::class.java)

    @Provides
    @Singleton
    fun providesGoalApiService(retrofit: Retrofit): GoalApiService =
        retrofit.create(GoalApiService::class.java)

    @Provides
    @Singleton
    fun providesDeviceIdProvider(
        @ApplicationContext context: Context,
    ): DeviceIdProvider = AndroidDeviceIdProvider(context)
}

package com.glucode.gautimes.di

import android.app.Application
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.data.remote.ApiKeyInterceptor
import com.glucode.gautimes.data.remote.TrainTimesApi
import com.glucode.gautimes.data.repository.DefaultHealthRepository
import com.glucode.gautimes.data.repository.DefaultJourneysRepository
import com.glucode.gautimes.data.repository.DefaultLocationRepository
import com.glucode.gautimes.data.repository.DefaultPermissionRepository
import com.glucode.gautimes.data.repository.DefaultStationsRepository
import com.glucode.gautimes.data.repository.DefaultUserSettingsRepository
import com.glucode.gautimes.data.repository.HealthRepository
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.data.repository.LocationRepository
import com.glucode.gautimes.data.repository.PermissionRepository
import com.glucode.gautimes.data.repository.StationsRepository
import com.glucode.gautimes.data.repository.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHealthRepository(
        repository: DefaultHealthRepository
    ): HealthRepository

    @Binds
    @Singleton
    abstract fun bindStationsRepository(
        repository: DefaultStationsRepository
    ): StationsRepository

    @Binds
    @Singleton
    abstract fun bindJourneysRepository(
        repository: DefaultJourneysRepository
    ): JourneysRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        repository: DefaultLocationRepository
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(
        repository: DefaultUserSettingsRepository
    ): UserSettingsRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        repository: DefaultPermissionRepository
    ): PermissionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    @OptIn(ExperimentalSerializationApi::class)
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideApiKeyInterceptor(): ApiKeyInterceptor =
        ApiKeyInterceptor(BuildConfig.TRAIN_TIMES_API_KEY)

    @Provides
    @Singleton
    fun provideHttpCache(application: Application): Cache =
        Cache(
            directory = File(application.cacheDir, "http_cache"),
            maxSize = HTTP_CACHE_BYTES
        )

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        apiKeyInterceptor: ApiKeyInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(apiKeyInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.TRAIN_TIMES_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideTrainTimesApi(retrofit: Retrofit): TrainTimesApi =
        retrofit.create(TrainTimesApi::class.java)

    private const val HTTP_CACHE_BYTES = 5L * 1024L * 1024L
}

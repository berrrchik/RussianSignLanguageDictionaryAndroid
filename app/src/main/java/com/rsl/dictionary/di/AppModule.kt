package com.rsl.dictionary.di

import android.content.Context
import android.content.SharedPreferences
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.rsl.dictionary.config.ApiConfig
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.repositories.decorators.LoggingSyncRepositoryDecorator
import com.rsl.dictionary.repositories.impl.FavoritesRepositoryImpl
import com.rsl.dictionary.repositories.impl.LessonRepositoryImpl
import com.rsl.dictionary.repositories.impl.SignRepositoryImpl
import com.rsl.dictionary.repositories.impl.SyncRepositoryImpl
import com.rsl.dictionary.repositories.impl.VideoRepositoryImpl
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import com.rsl.dictionary.repositories.protocols.LessonRepository
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.analytics.AnalyticsConsentService
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.analytics.CrashlyticsErrorReporter
import com.rsl.dictionary.services.analytics.PerformanceService
import com.rsl.dictionary.services.cache.CacheService
import com.rsl.dictionary.services.cache.VideoCacheDirectoryManager
import com.rsl.dictionary.services.cache.VideoCacheService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.services.network.ETagManager
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import com.rsl.dictionary.services.network.http.HttpResponseHandler
import com.rsl.dictionary.services.search.HybridSearchService
import com.rsl.dictionary.services.search.SBERTSearchService
import com.rsl.dictionary.utilities.cache.FileCacheLRU
import com.rsl.dictionary.utilities.cache.MemoryCacheManager
import com.rsl.dictionary.utilities.cache.VideoDownloadCoordinator
import com.rsl.dictionary.utilities.data.DataLoadCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json = ApiJsonDecoder.json

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.apiBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("rsl_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideETagManager(@ApplicationContext context: Context): ETagManager {
        return ETagManager(context)
    }

    @Provides
    @Singleton
    fun provideCacheService(@ApplicationContext context: Context): CacheService {
        return CacheService(context)
    }

    @Provides
    @Singleton
    fun provideVideoCacheDirectoryManager(): VideoCacheDirectoryManager = VideoCacheDirectoryManager

    @Provides
    @Singleton
    fun provideFileCacheLRU(): FileCacheLRU = FileCacheLRU

    @Provides
    @Singleton
    fun provideVideoDownloadCoordinator(): VideoDownloadCoordinator = VideoDownloadCoordinator()

    @Provides
    @Singleton
    fun provideMemoryCacheManager(): MemoryCacheManager<SyncData> = MemoryCacheManager()

    @Provides
    @Singleton
    fun provideDataLoadCoordinator(): DataLoadCoordinator<SyncData> = DataLoadCoordinator()

    @Provides
    @Singleton
    fun provideVideoCacheService(
        okHttpClient: OkHttpClient,
        lru: FileCacheLRU,
        coordinator: VideoDownloadCoordinator
    ): VideoCacheService {
        return VideoCacheService(okHttpClient, coordinator).also { lru }
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        okHttpClient: OkHttpClient,
        etagManager: ETagManager,
        handler: HttpResponseHandler
    ): SyncRepository {
        return LoggingSyncRepositoryDecorator(
            SyncRepositoryImpl(okHttpClient, etagManager, handler)
        )
    }

    @Provides
    @Singleton
    fun provideSignRepository(
        syncRepo: SyncRepository,
        cacheService: CacheService,
        networkMonitor: NetworkMonitor,
        memoryCache: MemoryCacheManager<SyncData>,
        coordinator: DataLoadCoordinator<SyncData>
    ): SignRepository {
        return SignRepositoryImpl(memoryCache, cacheService, syncRepo, networkMonitor, coordinator)
    }

    @Provides
    @Singleton
    fun provideVideoRepository(
        @ApplicationContext context: Context,
        cacheService: VideoCacheService,
        dirManager: VideoCacheDirectoryManager,
        networkMonitor: NetworkMonitor
    ): VideoRepository {
        return VideoRepositoryImpl(context, cacheService, networkMonitor).also { dirManager }
    }

    @Provides
    @Singleton
    fun provideLessonRepository(signRepo: SignRepository): LessonRepository {
        return LessonRepositoryImpl(signRepo)
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(sharedPrefs: SharedPreferences): FavoritesRepository {
        return FavoritesRepositoryImpl(sharedPrefs)
    }

    @Provides
    @Singleton
    fun provideCategoryService(signRepo: SignRepository): CategoryService {
        return CategoryService(signRepo)
    }

    @Provides
    @Singleton
    fun provideHybridSearchService(
        sbertService: SBERTSearchService,
        networkMonitor: NetworkMonitor
    ): HybridSearchService {
        return HybridSearchService(sbertService, networkMonitor)
    }

    @Provides
    @Singleton
    fun provideSBERTSearchService(
        okHttpClient: OkHttpClient,
        json: Json
    ): SBERTSearchService {
        return SBERTSearchService(okHttpClient).also { json }
    }

    @Provides
    @Singleton
    fun provideAnalyticsService(@ApplicationContext context: Context): AnalyticsService {
        return AnalyticsService(context)
    }

    @Provides
    @Singleton
    fun provideCrashlyticsErrorReporter(): CrashlyticsErrorReporter = CrashlyticsErrorReporter()

    @Provides
    @Singleton
    fun providePerformanceService(): PerformanceService = PerformanceService()

    @Provides
    @Singleton
    fun provideAnalyticsConsentService(): AnalyticsConsentService = AnalyticsConsentService()
}

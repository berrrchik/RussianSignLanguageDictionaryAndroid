package com.rsl.dictionary.di

import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import com.rsl.dictionary.repositories.protocols.LessonRepository
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.analytics.CrashlyticsErrorReporter
import com.rsl.dictionary.services.analytics.PerformanceService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.services.search.HybridSearchService
import javax.inject.Inject
import timber.log.Timber

class DIStartupValidation @Inject constructor(
    private val syncRepository: SyncRepository,
    private val signRepository: SignRepository,
    private val videoRepository: VideoRepository,
    private val lessonRepository: LessonRepository,
    private val favoritesRepository: FavoritesRepository,
    private val categoryService: CategoryService,
    private val hybridSearchService: HybridSearchService,
    private val analyticsService: AnalyticsService,
    private val crashlyticsErrorReporter: CrashlyticsErrorReporter,
    private val performanceService: PerformanceService
) {
    fun validate() {
        runCatching {
            requireNotNull(syncRepository)
            requireNotNull(signRepository)
            requireNotNull(videoRepository)
            requireNotNull(lessonRepository)
            requireNotNull(favoritesRepository)
            requireNotNull(categoryService)
            requireNotNull(hybridSearchService)
            requireNotNull(analyticsService)
            requireNotNull(crashlyticsErrorReporter)
            requireNotNull(performanceService)
            Timber.i("DI startup validation passed")
        }.onFailure { error ->
            Timber.e(error, "DI startup validation failed")
        }
    }
}

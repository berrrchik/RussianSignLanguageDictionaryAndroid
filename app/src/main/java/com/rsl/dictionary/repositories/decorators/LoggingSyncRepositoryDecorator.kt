package com.rsl.dictionary.repositories.decorators

import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.models.SyncMetadata
import com.rsl.dictionary.repositories.protocols.SyncRepository
import timber.log.Timber

class LoggingSyncRepositoryDecorator(
    private val delegate: SyncRepository
) : SyncRepository {

    override suspend fun checkForUpdates(lastUpdated: Long): SyncMetadata {
        Timber.d("SyncRepository.checkForUpdates(lastUpdated=%d)", lastUpdated)
        return runCatching { delegate.checkForUpdates(lastUpdated) }
            .onSuccess { result ->
                Timber.d(
                    "SyncRepository.checkForUpdates success(lastUpdated=%d, hasUpdates=%s)",
                    result.lastUpdated,
                    result.hasUpdates
                )
            }
            .onFailure { error ->
                Timber.e(error, "SyncRepository.checkForUpdates failed")
            }
            .getOrThrow()
    }

    override suspend fun fetchAllData(cachedDataProvider: (() -> SyncData?)?): SyncData {
        Timber.d("SyncRepository.fetchAllData()")
        return runCatching { delegate.fetchAllData(cachedDataProvider) }
            .onSuccess { result ->
                Timber.d(
                    "SyncRepository.fetchAllData success(categories=%d, signs=%d, lessons=%d, lastUpdated=%d)",
                    result.categories.size,
                    result.signs.size,
                    result.lessons.size,
                    result.lastUpdated
                )
            }
            .onFailure { error ->
                Timber.e(error, "SyncRepository.fetchAllData failed")
            }
            .getOrThrow()
    }
}

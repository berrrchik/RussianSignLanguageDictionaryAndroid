package com.rsl.dictionary.repositories.protocols

import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.models.SyncMetadata

interface SyncRepository {
    suspend fun checkForUpdates(lastUpdated: Long): SyncMetadata
    suspend fun fetchAllData(cachedDataProvider: (() -> SyncData?)?): SyncData
}

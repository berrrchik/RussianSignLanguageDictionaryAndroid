package com.rsl.dictionary.testing.fakes

import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.models.SyncMetadata
import com.rsl.dictionary.repositories.protocols.SyncFetchResult
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.testing.factories.TestDataFactory

class FakeSyncRepository(
    var metadata: SyncMetadata = SyncMetadata(
        lastUpdated = TestDataFactory.syncData().lastUpdated,
        hasUpdates = false
    ),
    var syncData: SyncData = TestDataFactory.syncData()
) : SyncRepository {
    val checkedTimestamps = mutableListOf<Long>()
    var fetchAllDataCalls: Int = 0
        private set

    override suspend fun checkForUpdates(lastUpdated: Long): SyncMetadata {
        checkedTimestamps += lastUpdated
        return metadata
    }

    override suspend fun fetchAllData(cachedDataProvider: (() -> SyncData?)?): SyncFetchResult {
        fetchAllDataCalls += 1
        return SyncFetchResult.Updated(syncData)
    }
}

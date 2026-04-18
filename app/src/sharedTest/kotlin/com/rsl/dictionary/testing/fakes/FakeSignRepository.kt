package com.rsl.dictionary.testing.fakes

import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.testing.factories.TestDataFactory

class FakeSignRepository(
    private var currentData: SyncData = TestDataFactory.syncData()
) : SignRepository {
    var loadDataWithSyncCalls: Int = 0
        private set

    override suspend fun loadDataWithSync(): SyncData {
        loadDataWithSyncCalls += 1
        return currentData
    }

    override suspend fun getSign(byId: String): Sign {
        return currentData.signs.firstOrNull { it.id == byId }
            ?: error("Sign with id=$byId was not found in FakeSignRepository")
    }

    override suspend fun getAllSigns(): List<Sign> = currentData.signs

    override suspend fun getSignsByCategory(categoryId: String): List<Sign> {
        return currentData.signs.filter { it.categoryId == categoryId }
    }

    fun replaceData(updatedData: SyncData) {
        currentData = updatedData
    }
}

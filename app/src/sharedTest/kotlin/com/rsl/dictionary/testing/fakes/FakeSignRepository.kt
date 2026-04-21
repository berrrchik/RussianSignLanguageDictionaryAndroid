package com.rsl.dictionary.testing.fakes

import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.testing.factories.TestDataFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSignRepository(
    private var currentData: SyncData = TestDataFactory.syncData()
) : SignRepository {
    var loadDataWithSyncCalls: Int = 0
        private set
    val refreshCalls = mutableListOf<RefreshReason>()

    private val _syncData = MutableStateFlow<SyncData?>(currentData)
    override val syncData: StateFlow<SyncData?> = _syncData.asStateFlow()

    private val _dataStatus = MutableStateFlow<RepositoryDataStatus>(RepositoryDataStatus.Idle)
    override val dataStatus: StateFlow<RepositoryDataStatus> = _dataStatus.asStateFlow()

    private val _refreshState = MutableStateFlow<SignRepositoryRefreshState>(
        SignRepositoryRefreshState.Idle
    )
    override val refreshState: StateFlow<SignRepositoryRefreshState> = _refreshState.asStateFlow()

    var nextRefreshResult: RefreshResult = RefreshResult.NotModified(currentData)

    override suspend fun loadDataWithSync(): SyncData {
        loadDataWithSyncCalls += 1
        _syncData.value = currentData
        return currentData
    }

    override suspend fun refresh(reason: RefreshReason): RefreshResult {
        refreshCalls += reason
        _refreshState.value = SignRepositoryRefreshState.Refreshing(reason)
        val result = nextRefreshResult.normalize(currentData)
        when (result) {
            is RefreshResult.Updated -> replaceData(result.data)
            is RefreshResult.NotModified -> replaceData(result.data)
            is RefreshResult.UsedCachedData -> replaceData(result.data)
            else -> Unit
        }
        _dataStatus.value = result.toRepositoryDataStatus()
        _refreshState.value = SignRepositoryRefreshState.Completed(reason, result)
        return result
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
        _syncData.value = updatedData
        nextRefreshResult = RefreshResult.NotModified(updatedData)
    }

    private fun RefreshResult.normalize(currentData: SyncData): RefreshResult = when (this) {
        is RefreshResult.NotModified -> RefreshResult.NotModified(currentData)
        is RefreshResult.UsedCachedData -> RefreshResult.UsedCachedData(currentData, reason)
        else -> this
    }

    private fun RefreshResult.toRepositoryDataStatus(): RepositoryDataStatus = when (this) {
        is RefreshResult.Updated -> RepositoryDataStatus.Updated
        is RefreshResult.NotModified -> RepositoryDataStatus.UpToDate
        is RefreshResult.UsedCachedData -> RepositoryDataStatus.UsingCachedData(
            when (reason) {
                com.rsl.dictionary.repositories.protocols.CachedDataReason.NO_INTERNET ->
                    com.rsl.dictionary.models.DataStatusReason.NoInternet

                else -> com.rsl.dictionary.models.DataStatusReason.ServerUnavailable
            }
        )

        is RefreshResult.NoInternet -> RepositoryDataStatus.NoData(
            com.rsl.dictionary.models.DataStatusReason.NoInternet
        )

        else -> RepositoryDataStatus.NoData(
            com.rsl.dictionary.models.DataStatusReason.ServerUnavailable
        )
    }
}

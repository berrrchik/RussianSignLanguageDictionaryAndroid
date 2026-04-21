package com.rsl.dictionary.repositories.protocols

import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SyncData
import kotlinx.coroutines.flow.StateFlow

enum class RefreshReason {
    STARTUP,
    LOAD_MISS,
    BACKGROUND,
    MANUAL_RETRY_AFTER_ERROR
}

enum class CachedDataReason {
    NO_INTERNET,
    NETWORK_ERROR,
    SERVER_UNAVAILABLE,
    DECODING_ERROR,
    UNKNOWN_ERROR
}

sealed interface RefreshResult {
    data class Updated(val data: SyncData) : RefreshResult
    data class NotModified(val data: SyncData) : RefreshResult
    data class UsedCachedData(
        val data: SyncData,
        val reason: CachedDataReason
    ) : RefreshResult

    data object NoInternet : RefreshResult
    data object ServerUnavailable : RefreshResult
    data class NetworkError(val cause: Throwable) : RefreshResult
    data class DecodingError(val cause: Throwable) : RefreshResult
    data class UnknownError(val cause: Throwable? = null) : RefreshResult
}

sealed interface SignRepositoryRefreshState {
    data object Idle : SignRepositoryRefreshState
    data class Refreshing(val reason: RefreshReason) : SignRepositoryRefreshState
    data class Completed(
        val reason: RefreshReason,
        val result: RefreshResult
    ) : SignRepositoryRefreshState
}

interface SignRepository {
    val syncData: StateFlow<SyncData?>
    val dataStatus: StateFlow<RepositoryDataStatus>
    val refreshState: StateFlow<SignRepositoryRefreshState>

    suspend fun loadDataWithSync(): SyncData
    suspend fun refresh(reason: RefreshReason): RefreshResult
    suspend fun getSign(byId: String): Sign
    suspend fun getAllSigns(): List<Sign>
    suspend fun getSignsByCategory(categoryId: String): List<Sign>
}

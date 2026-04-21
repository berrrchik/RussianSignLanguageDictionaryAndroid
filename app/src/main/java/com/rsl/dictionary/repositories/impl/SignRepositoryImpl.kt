package com.rsl.dictionary.repositories.impl

import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.repositories.protocols.CachedDataReason
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.repositories.protocols.SyncFetchResult
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.services.cache.CacheService
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.utilities.cache.MemoryCacheManager
import com.rsl.dictionary.utilities.data.DataLoadCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class SignRepositoryImpl @Inject constructor(
    private val memoryCacheManager: MemoryCacheManager<SyncData>,
    private val cacheService: CacheService,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor,
    private val dataLoadCoordinator: DataLoadCoordinator<SyncData>
) : SignRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private var backgroundRefreshJob: Job? = null

    private val _syncData = MutableStateFlow<SyncData?>(null)
    override val syncData: StateFlow<SyncData?> = _syncData.asStateFlow()

    private val _dataStatus = MutableStateFlow<RepositoryDataStatus>(RepositoryDataStatus.Idle)
    override val dataStatus: StateFlow<RepositoryDataStatus> = _dataStatus.asStateFlow()

    private val _refreshState = MutableStateFlow<SignRepositoryRefreshState>(
        SignRepositoryRefreshState.Idle
    )
    override val refreshState: StateFlow<SignRepositoryRefreshState> = _refreshState.asStateFlow()

    override suspend fun loadDataWithSync(): SyncData {
        try {
            memoryCacheManager.get()?.let { cachedData ->
                publishCurrentData(cachedData)
                Timber.d(
                    "SignRepository: returning data from memory cache signs=%d categories=%d lessons=%d",
                    cachedData.signs.size,
                    cachedData.categories.size,
                    cachedData.lessons.size
                )
                launchBackgroundSync()
                return cachedData
            }

            var shouldLaunchBackgroundSync = false

            val data = coroutineScope {
                dataLoadCoordinator.load(this) {
                    cacheService.load()?.also { cachedData ->
                        publishCurrentData(cachedData)
                        Timber.d(
                            "SignRepository: returning data from disk cache signs=%d categories=%d lessons=%d",
                            cachedData.signs.size,
                            cachedData.categories.size,
                            cachedData.lessons.size
                        )
                        shouldLaunchBackgroundSync = true
                    } ?: loadFromServer()
                }
            }

            if (shouldLaunchBackgroundSync) {
                launchBackgroundSync()
            }

            return data
        } catch (error: SignRepositoryError) {
            throw error
        } catch (error: SyncError.NetworkError) {
            throw SignRepositoryError.NetworkError(error.cause)
        } catch (error: SyncError.DecodingError) {
            throw SignRepositoryError.DecodingError(error.cause)
        } catch (error: SyncError.ServerUnavailable) {
            throw SignRepositoryError.ServerUnavailable
        } catch (error: SyncError.NoInternet) {
            throw SignRepositoryError.NoDataAvailable
        } catch (_: SyncError) {
            throw SignRepositoryError.UnknownError
        } catch (_: Exception) {
            throw SignRepositoryError.UnknownError
        }
    }

    override suspend fun getSign(byId: String): Sign {
        return loadDataWithSync().signs.firstOrNull { it.id == byId }
            ?: throw SignRepositoryError.NotFound
    }

    override suspend fun getAllSigns(): List<Sign> = loadDataWithSync().signs

    override suspend fun getSignsByCategory(categoryId: String): List<Sign> {
        return getAllSigns().filter { it.categoryId == categoryId }
    }

    override suspend fun refresh(reason: RefreshReason): RefreshResult = refreshMutex.withLock {
        _refreshState.value = SignRepositoryRefreshState.Refreshing(reason)

        val currentSnapshot = currentSnapshot()
        val result = performRefresh(currentSnapshot)
        _dataStatus.value = result.toRepositoryDataStatus()

        _refreshState.value = SignRepositoryRefreshState.Completed(reason, result)
        result
    }

    private suspend fun loadFromServer(): SyncData {
        return when (val result = refresh(RefreshReason.LOAD_MISS)) {
            is RefreshResult.Updated -> result.data
            is RefreshResult.NotModified -> result.data
            is RefreshResult.UsedCachedData -> result.data
            is RefreshResult.NoInternet -> {
                Timber.w("SignRepository: no network and no local sync cache available")
                throw SignRepositoryError.NoDataAvailable
            }

            is RefreshResult.ServerUnavailable -> throw SignRepositoryError.ServerUnavailable
            is RefreshResult.NetworkError -> throw SignRepositoryError.NetworkError(result.cause)
            is RefreshResult.DecodingError -> throw SignRepositoryError.DecodingError(result.cause)
            is RefreshResult.UnknownError -> throw SignRepositoryError.UnknownError
        }
    }

    private fun launchBackgroundSync() {
        if (backgroundRefreshJob?.isActive == true || refreshMutex.isLocked) return

        backgroundRefreshJob = repositoryScope.launch {
            runCatching { refresh(RefreshReason.BACKGROUND) }
                .onFailure { error ->
                    Timber.e(error, "Background sync failed")
                }
        }
    }

    private suspend fun performRefresh(currentSnapshot: SyncData?): RefreshResult {
        if (!networkMonitor.isConnected()) {
            return currentSnapshot?.let {
                RefreshResult.UsedCachedData(it, CachedDataReason.NO_INTERNET)
            } ?: RefreshResult.NoInternet
        }

        return try {
            Timber.d("SignRepository: refreshing sync data")
            when (val result = syncRepository.fetchAllData { currentSnapshot }) {
                is SyncFetchResult.Updated -> {
                    updateCaches(result.data)
                    RefreshResult.Updated(result.data)
                }

                is SyncFetchResult.NotModified -> {
                    publishCurrentData(result.data)
                    RefreshResult.NotModified(result.data)
                }
            }
        } catch (error: SyncError.NoInternet) {
            currentSnapshot?.let {
                RefreshResult.UsedCachedData(it, CachedDataReason.NO_INTERNET)
            } ?: RefreshResult.NoInternet
        } catch (error: SyncError.ServerUnavailable) {
            currentSnapshot?.let {
                RefreshResult.UsedCachedData(it, CachedDataReason.SERVER_UNAVAILABLE)
            } ?: RefreshResult.ServerUnavailable
        } catch (error: SyncError.NetworkError) {
            currentSnapshot?.let {
                RefreshResult.UsedCachedData(it, CachedDataReason.NETWORK_ERROR)
            } ?: RefreshResult.NetworkError(error.cause)
        } catch (error: SyncError.DecodingError) {
            currentSnapshot?.let {
                RefreshResult.UsedCachedData(it, CachedDataReason.DECODING_ERROR)
            } ?: RefreshResult.DecodingError(error.cause)
        } catch (error: SyncError) {
            currentSnapshot?.let {
                RefreshResult.UsedCachedData(it, CachedDataReason.UNKNOWN_ERROR)
            } ?: RefreshResult.UnknownError(error.cause)
        } catch (error: Exception) {
            currentSnapshot?.let {
                RefreshResult.UsedCachedData(it, CachedDataReason.UNKNOWN_ERROR)
            } ?: RefreshResult.UnknownError(error)
        }
    }

    private suspend fun updateCaches(data: SyncData) {
        memoryCacheManager.set(data)
        cacheService.save(data)
        _syncData.value = data
        Timber.d(
            "SignRepository: updated caches signs=%d categories=%d lessons=%d lastUpdated=%d",
            data.signs.size,
            data.categories.size,
            data.lessons.size,
            data.lastUpdated
        )
    }

    private suspend fun currentSnapshot(): SyncData? {
        memoryCacheManager.get()?.let { cachedData ->
            publishCurrentData(cachedData)
            return cachedData
        }

        return cacheService.load()?.also { cachedData ->
            publishCurrentData(cachedData)
        }
    }

    private suspend fun publishCurrentData(data: SyncData) {
        memoryCacheManager.set(data)
        _syncData.value = data
    }

    private fun RefreshResult.toRepositoryDataStatus(): RepositoryDataStatus = when (this) {
        is RefreshResult.Updated -> RepositoryDataStatus.Updated
        is RefreshResult.NotModified -> RepositoryDataStatus.UpToDate
        is RefreshResult.UsedCachedData -> RepositoryDataStatus.UsingCachedData(
            reason = reason.toDataStatusReason()
        )

        is RefreshResult.NoInternet -> RepositoryDataStatus.NoData(DataStatusReason.NoInternet)
        is RefreshResult.ServerUnavailable,
        is RefreshResult.NetworkError,
        is RefreshResult.DecodingError,
        is RefreshResult.UnknownError -> RepositoryDataStatus.NoData(DataStatusReason.ServerUnavailable)
    }

    private fun CachedDataReason.toDataStatusReason(): DataStatusReason = when (this) {
        CachedDataReason.NO_INTERNET -> DataStatusReason.NoInternet
        CachedDataReason.NETWORK_ERROR,
        CachedDataReason.SERVER_UNAVAILABLE,
        CachedDataReason.DECODING_ERROR,
        CachedDataReason.UNKNOWN_ERROR -> DataStatusReason.ServerUnavailable
    }
}

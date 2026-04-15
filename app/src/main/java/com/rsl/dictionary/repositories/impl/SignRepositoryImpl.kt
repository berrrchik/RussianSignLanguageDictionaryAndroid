package com.rsl.dictionary.repositories.impl

import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.services.cache.CacheService
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.utilities.cache.MemoryCacheManager
import com.rsl.dictionary.utilities.data.DataLoadCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class SignRepositoryImpl @Inject constructor(
    private val memoryCacheManager: MemoryCacheManager<SyncData>,
    private val cacheService: CacheService,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor,
    private val dataLoadCoordinator: DataLoadCoordinator<SyncData>
) : SignRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun loadDataWithSync(): SyncData {
        try {
            memoryCacheManager.get()?.let { cachedData ->
                launchBackgroundSync()
                return cachedData
            }

            var shouldLaunchBackgroundSync = false

            val data = coroutineScope {
                dataLoadCoordinator.load(this) {
                    cacheService.load()?.also { cachedData ->
                        memoryCacheManager.set(cachedData)
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

    private suspend fun loadFromServer(): SyncData {
        if (!networkMonitor.isConnected()) {
            throw SignRepositoryError.NoDataAvailable
        }

        val data = syncRepository.fetchAllData(null)
        updateCaches(data)
        return data
    }

    private fun launchBackgroundSync() {
        repositoryScope.launch {
            runCatching { performBackgroundSync() }
                .onFailure { error ->
                    Timber.e(error, "Background sync failed")
                }
        }
    }

    private suspend fun performBackgroundSync() {
        if (!networkMonitor.isConnected()) return

        val currentData = memoryCacheManager.get()
        val refreshedData = syncRepository.fetchAllData {
            runBlocking { memoryCacheManager.get() }
        }

        if (currentData != refreshedData) {
            updateCaches(refreshedData)
        }
    }

    private suspend fun updateCaches(data: SyncData) {
        memoryCacheManager.set(data)
        cacheService.save(data)
    }
}

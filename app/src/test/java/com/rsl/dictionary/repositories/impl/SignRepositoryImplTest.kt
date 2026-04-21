package com.rsl.dictionary.repositories.impl

import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.repositories.protocols.CachedDataReason
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.repositories.protocols.SyncFetchResult
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.services.cache.CacheService
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import com.rsl.dictionary.utilities.cache.MemoryCacheManager
import com.rsl.dictionary.utilities.data.DataLoadCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun memoryCacheHit_returnsDataWithoutDiskOrNetworkFetch() = runTest {
        val data = TestDataFactory.syncData()
        val memoryCache = MemoryCacheManager<SyncData>().also { it.set(data) }
        val cacheService = mockk<CacheService>()
        val syncRepository = mockk<SyncRepository>()
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns false
        }

        val repository = repository(memoryCache, cacheService, syncRepository, networkMonitor)

        val result = repository.loadDataWithSync()

        assertSame(data, result)
        coVerify(exactly = 0) { cacheService.load() }
        coVerify(exactly = 0) { syncRepository.fetchAllData(any()) }
    }

    @Test
    fun diskCacheHit_warmsMemoryCache() = runTest {
        val data = TestDataFactory.syncData()
        val memoryCache = MemoryCacheManager<SyncData>()
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns data
        }
        val syncRepository = mockk<SyncRepository>()
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns false
        }

        val repository = repository(memoryCache, cacheService, syncRepository, networkMonitor)

        val result = repository.loadDataWithSync()

        assertSame(data, result)
        assertSame(data, memoryCache.get())
        coVerify(exactly = 1) { cacheService.load() }
        coVerify(exactly = 0) { syncRepository.fetchAllData(any()) }
    }

    @Test
    fun diskCacheHit_launchesBackgroundSyncAfterReturningData() = runTest {
        val cachedData = TestDataFactory.syncData(lastUpdated = 1)
        val refreshedData = TestDataFactory.syncData(lastUpdated = 2)
        val memoryCache = MemoryCacheManager<SyncData>()
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns cachedData
            coEvery { save(refreshedData) } returns Unit
        }
        val backgroundSyncStarted = CountDownLatch(1)
        val backgroundSyncRelease = CountDownLatch(1)
        val syncRepository = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } coAnswers {
                backgroundSyncStarted.countDown()
                backgroundSyncRelease.await(2, TimeUnit.SECONDS)
                SyncFetchResult.Updated(refreshedData)
            }
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }

        val repository = repository(memoryCache, cacheService, syncRepository, networkMonitor)

        val result = repository.loadDataWithSync()

        assertSame(cachedData, result)
        assertTrue(backgroundSyncStarted.await(2, TimeUnit.SECONDS))

        backgroundSyncRelease.countDown()

        coVerify(timeout = 2_000, exactly = 1) { syncRepository.fetchAllData(any()) }
        coVerify(timeout = 2_000) { cacheService.save(refreshedData) }
    }

    @Test
    fun cacheMissOnline_fetchesFromServerAndUpdatesBothCaches() = runTest {
        val serverData = TestDataFactory.syncData()
        val memoryCache = MemoryCacheManager<SyncData>()
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns null
            coEvery { save(serverData) } returns Unit
        }
        val syncRepository = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } returns SyncFetchResult.Updated(serverData)
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }

        val repository = repository(memoryCache, cacheService, syncRepository, networkMonitor)

        val result = repository.loadDataWithSync()

        assertSame(serverData, result)
        assertSame(serverData, memoryCache.get())
        coVerify(exactly = 1) { cacheService.save(serverData) }
    }

    @Test(expected = SignRepositoryError.NoDataAvailable::class)
    fun cacheMissOffline_throwsNoDataAvailable() = runTest {
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns null
        }
        val syncRepository = mockk<SyncRepository>()
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns false
        }

        repository(
            MemoryCacheManager(),
            cacheService,
            syncRepository,
            networkMonitor
        ).loadDataWithSync()
    }

    @Test
    fun syncNetworkError_mapsToSignRepositoryNetworkError() = runTest {
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns null
        }
        val expectedCause = IOException("offline")
        val syncRepository = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } throws SyncError.NetworkError(expectedCause)
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }

        val error = runCatching {
            repository(MemoryCacheManager(), cacheService, syncRepository, networkMonitor)
                .loadDataWithSync()
        }.exceptionOrNull()

        assertTrue(error is SignRepositoryError.NetworkError)
        assertSame(expectedCause, error?.cause)
    }

    @Test
    fun syncDecodingError_mapsToSignRepositoryDecodingError() = runTest {
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns null
        }
        val expectedCause = IllegalArgumentException("bad json")
        val syncRepository = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } throws SyncError.DecodingError(expectedCause)
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }

        val error = runCatching {
            repository(MemoryCacheManager(), cacheService, syncRepository, networkMonitor)
                .loadDataWithSync()
        }.exceptionOrNull()

        assertTrue(error is SignRepositoryError.DecodingError)
        assertSame(expectedCause, error?.cause)
    }

    @Test
    fun syncServerUnavailable_mapsToSignRepositoryServerUnavailable() = runTest {
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns null
        }
        val syncRepository = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } throws SyncError.ServerUnavailable
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }

        val error = runCatching {
            repository(MemoryCacheManager(), cacheService, syncRepository, networkMonitor)
                .loadDataWithSync()
        }.exceptionOrNull()

        assertTrue(error is SignRepositoryError.ServerUnavailable)
    }

    @Test
    fun concurrentLoadDataWithSync_doesNotDuplicateServerLoad() = runTest {
        val serverData = TestDataFactory.syncData()
        val cacheService = mockk<CacheService> {
            coEvery { load() } returns null
            coEvery { save(serverData) } returns Unit
        }
        val calls = AtomicInteger(0)
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val syncRepository = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } coAnswers {
                calls.incrementAndGet()
                started.complete(Unit)
                release.await()
                SyncFetchResult.Updated(serverData)
            }
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }
        val repository = repository(
            MemoryCacheManager(),
            cacheService,
            syncRepository,
            networkMonitor
        )

        val first = async { repository.loadDataWithSync() }
        val second = async { repository.loadDataWithSync() }

        started.await()
        assertEquals(1, calls.get())

        release.complete(Unit)

        assertSame(serverData, first.await())
        assertSame(serverData, second.await())
        assertEquals(1, calls.get())
    }

    @Test
    fun refresh_notModified_emitsCompletedRefreshState() = runTest {
        val cachedData = TestDataFactory.syncData(lastUpdated = 7L)
        val memoryCache = MemoryCacheManager<SyncData>().also { it.set(cachedData) }
        val cacheService = mockk<CacheService>(relaxed = true)
        val syncRepository = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } returns SyncFetchResult.NotModified(cachedData)
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }

        val repository = repository(memoryCache, cacheService, syncRepository, networkMonitor)

        val result = repository.refresh(RefreshReason.STARTUP)

        assertEquals(RefreshResult.NotModified(cachedData), result)
        assertEquals(
            SignRepositoryRefreshState.Completed(RefreshReason.STARTUP, result),
            repository.refreshState.value
        )
        assertSame(cachedData, repository.syncData.value)
    }

    @Test
    fun refresh_withoutNetwork_usesCachedDataWhenAvailable() = runTest {
        val cachedData = TestDataFactory.syncData(lastUpdated = 7L)
        val memoryCache = MemoryCacheManager<SyncData>().also { it.set(cachedData) }
        val repository = repository(
            memoryCache = memoryCache,
            cacheService = mockk(relaxed = true),
            syncRepository = mockk(relaxed = true),
            networkMonitor = mockk {
                every { isConnected() } returns false
            }
        )

        val result = repository.refresh(RefreshReason.BACKGROUND)

        assertEquals(
            RefreshResult.UsedCachedData(cachedData, CachedDataReason.NO_INTERNET),
            result
        )
        assertEquals(
            RepositoryDataStatus.UsingCachedData(DataStatusReason.NoInternet),
            repository.dataStatus.value
        )
    }

    @Test
    fun refresh_withCachedDataAndServerFailure_marksServerUnavailableCachedMode() = runTest {
        val cachedData = TestDataFactory.syncData(lastUpdated = 7L)
        val memoryCache = MemoryCacheManager<SyncData>().also { it.set(cachedData) }
        val repository = repository(
            memoryCache = memoryCache,
            cacheService = mockk(relaxed = true),
            syncRepository = mockk {
                coEvery { fetchAllData(any()) } throws SyncError.ServerUnavailable
            },
            networkMonitor = mockk {
                every { isConnected() } returns true
            }
        )

        val result = repository.refresh(RefreshReason.BACKGROUND)

        assertEquals(
            RefreshResult.UsedCachedData(cachedData, CachedDataReason.SERVER_UNAVAILABLE),
            result
        )
        assertEquals(
            RepositoryDataStatus.UsingCachedData(DataStatusReason.ServerUnavailable),
            repository.dataStatus.value
        )
    }

    private fun repository(
        memoryCache: MemoryCacheManager<SyncData>,
        cacheService: CacheService,
        syncRepository: SyncRepository,
        networkMonitor: NetworkMonitor
    ): SignRepositoryImpl {
        return SignRepositoryImpl(
            memoryCacheManager = memoryCache,
            cacheService = cacheService,
            syncRepository = syncRepository,
            networkMonitor = networkMonitor,
            dataLoadCoordinator = DataLoadCoordinator()
        )
    }
}

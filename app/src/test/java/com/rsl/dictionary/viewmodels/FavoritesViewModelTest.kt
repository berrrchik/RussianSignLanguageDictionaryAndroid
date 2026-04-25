package com.rsl.dictionary.viewmodels

import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.fakes.FakeFavoritesRepository
import com.rsl.dictionary.testing.fakes.FakeSignRepository
import com.rsl.dictionary.testing.fakes.FakeVideoRepository
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signApple = TestDataFactory.sign(id = "sign-1", word = "Арбуз")
    private val signBanana = TestDataFactory.sign(id = "sign-2", word = "Банан")
    private val syncData = TestDataFactory.syncData(signs = listOf(signBanana, signApple))
    private val networkMonitor = mockk<NetworkMonitor> {
        every { isConnectedFlow } returns MutableStateFlow(true)
    }

    @Test
    fun favoritesFlow_updatesFavoritesAndGroupedSections() = runTest {
        val favoritesRepository = FakeFavoritesRepository(listOf("sign-2"))
        val viewModel = FavoritesViewModel(
            favoritesRepository = favoritesRepository,
            signRepository = FakeSignRepository(syncData),
            videoRepository = FakeVideoRepository(),
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        assertEquals(listOf("Банан"), viewModel.favorites.value.map { it.word })

        favoritesRepository.markFavoritePending(signApple)
        advanceUntilIdle()

        assertEquals(listOf("Банан", "Арбуз"), viewModel.favorites.value.map { it.word })
        assertEquals(listOf("А", "Б"), viewModel.groupedFavorites.value.keys.toList())
        assertEquals(FavoriteOfflineStatus.PENDING, viewModel.favoriteStatuses.value["sign-1"])

        favoritesRepository.remove("sign-2")
        advanceUntilIdle()

        assertEquals(listOf("Арбуз"), viewModel.favorites.value.map { it.word })
    }

    @Test
    fun clearAll_clearsFavoritesCacheForAllLoadedSigns() = runTest {
        val favoritesRepository = FakeFavoritesRepository(listOf("sign-2", "sign-1"))
        val videoRepository = FakeVideoRepository()
        val viewModel = FavoritesViewModel(
            favoritesRepository = favoritesRepository,
            signRepository = FakeSignRepository(syncData),
            videoRepository = videoRepository,
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        viewModel.clearAll()
        advanceUntilIdle()

        assertEquals(listOf("sign-2", "sign-1"), videoRepository.clearedFavoritesSignIds)
        assertEquals(emptyList<String>(), favoritesRepository.getAll())
    }

    @Test
    fun clearAllFailure_mapsError() = runTest {
        val favoritesRepository = FakeFavoritesRepository(listOf("sign-1"))
        val entry = favoritesRepository.getEntries().single()
        val videoRepository = mockk<VideoRepository> {
            coEvery { removeFavoriteOffline(entry) } throws
                VideoRepositoryError.CacheError(IOException("disk"))
        }
        val viewModel = FavoritesViewModel(
            favoritesRepository = favoritesRepository,
            signRepository = FakeSignRepository(
                TestDataFactory.syncData(signs = listOf(signApple))
            ),
            videoRepository = videoRepository,
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        viewModel.clearAll()
        advanceUntilIdle()

        assertEquals("Видео сейчас недоступно", viewModel.error.value)
    }

    @Test
    fun retryLoadingFavorites_reloadsAfterPreviousFailure() = runTest {
        val favoritesRepository = FakeFavoritesRepository(listOf("sign-1"))
        val signRepository = mockk<SignRepository> {
            every { dataStatus } returns MutableStateFlow(RepositoryDataStatus.Idle)
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
            coEvery { getAllSigns() } throws RuntimeException("boom") andThen listOf(signApple)
            coEvery { refresh(any()) } returns RefreshResult.NoInternet
        }
        val viewModel = FavoritesViewModel(
            favoritesRepository = favoritesRepository,
            signRepository = signRepository,
            videoRepository = FakeVideoRepository(),
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        assertEquals("Произошла неизвестная ошибка", viewModel.error.value)

        viewModel.retryLoadingFavorites()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
        assertEquals(listOf("Арбуз"), viewModel.favorites.value.map { it.word })
    }

    @Test
    fun loadFailure_usesCachedFavoriteSnapshotsWhenAvailable() = runTest {
        val favoritesRepository = FakeFavoritesRepository()
        favoritesRepository.markFavoritePending(signApple)
        val signRepository = mockk<SignRepository> {
            every { dataStatus } returns MutableStateFlow(RepositoryDataStatus.Idle)
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
            coEvery { getAllSigns() } throws SignRepositoryError.NoDataAvailable
            coEvery { refresh(any()) } returns RefreshResult.NoInternet
        }
        val viewModel = FavoritesViewModel(
            favoritesRepository = favoritesRepository,
            signRepository = signRepository,
            videoRepository = FakeVideoRepository(),
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        assertNull(viewModel.error.value)
        assertEquals(listOf("Арбуз"), viewModel.favorites.value.map { it.word })
        assertEquals(listOf("А"), viewModel.groupedFavorites.value.keys.toList())
    }

    @Test
    fun cachedMetadata_canRemainVisibleWhileOfflineMediaIsNotReady() = runTest {
        val favoritesRepository = FakeFavoritesRepository()
        favoritesRepository.markFavoritePending(signApple)
        favoritesRepository.markFavoriteFailed("sign-1", emptyList())
        val signRepository = mockk<SignRepository> {
            every { dataStatus } returns MutableStateFlow(RepositoryDataStatus.Idle)
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
            coEvery { getAllSigns() } throws SignRepositoryError.NoDataAvailable
            coEvery { refresh(any()) } returns RefreshResult.NoInternet
        }
        val viewModel = FavoritesViewModel(
            favoritesRepository = favoritesRepository,
            signRepository = signRepository,
            videoRepository = FakeVideoRepository(),
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        assertEquals(listOf("Арбуз"), viewModel.favorites.value.map { it.word })
        assertEquals(FavoriteOfflineStatus.FAILED, viewModel.favoriteStatuses.value["sign-1"])
        assertNull(viewModel.error.value)
    }

    @Test
    fun repositoryUpdate_refreshesVisibleFavorites() = runTest {
        val favoritesRepository = FakeFavoritesRepository(listOf("sign-1"))
        val signRepository = FakeSignRepository(TestDataFactory.syncData(signs = listOf(signApple)))
        val viewModel = FavoritesViewModel(
            favoritesRepository = favoritesRepository,
            signRepository = signRepository,
            videoRepository = FakeVideoRepository(),
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        signRepository.replaceData(TestDataFactory.syncData(signs = listOf(signBanana)))
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.favorites.value.map { it.word })
    }
}

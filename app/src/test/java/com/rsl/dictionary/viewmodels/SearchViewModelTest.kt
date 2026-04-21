package com.rsl.dictionary.viewmodels

import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.ScreenDataStatus
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.services.search.HybridSearchService
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.fakes.FakeSignRepository
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import com.rsl.dictionary.utilities.data.SortOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryA = TestDataFactory.category(id = "category-a", name = "А", order = 1)
    private val categoryB = TestDataFactory.category(id = "category-b", name = "Б", order = 2)
    private val signApple = TestDataFactory.sign(
        id = "sign-1",
        word = "Арбуз",
        categoryId = "category-a"
    )
    private val signBanana = TestDataFactory.sign(
        id = "sign-2",
        word = "Банан",
        categoryId = "category-b"
    )
    private val signHome = TestDataFactory.sign(
        id = "sign-3",
        word = "Дом",
        categoryId = "category-b"
    )
    private val syncData = TestDataFactory.syncData(
        categories = listOf(categoryB, categoryA),
        signs = listOf(signBanana, signHome, signApple)
    )

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    @Test
    fun initialLoad_fetchesCategoriesAndStartingAlphabeticalList() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val hybridSearchService = mockk<HybridSearchService>(relaxed = true)

        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = hybridSearchService,
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        assertEquals(listOf(categoryA, categoryB), viewModel.categories.value)
        assertEquals(
            listOf("Арбуз", "Банан", "Дом"),
            viewModel.searchResults.value.map(Sign::word)
        )
    }

    @Test
    fun searchQuery_respectsThreeHundredMillisecondDebounce() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val hybridSearchService = mockk<HybridSearchService> {
            coEvery { performHybridSearch("дом", any()) } returns listOf(signHome)
        }
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = hybridSearchService,
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        viewModel.searchQuery.value = "дом"

        advanceTimeBy(299)
        coVerify(exactly = 0) { hybridSearchService.performHybridSearch("дом", any()) }

        advanceTimeBy(1)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            hybridSearchService.performHybridSearch("дом", listOf(signBanana, signHome, signApple))
        }
    }

    @Test
    fun emptyQuery_buildsGroupedResultsByLetters() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = mockk(relaxed = true),
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        assertEquals(listOf("А", "Б", "Д"), viewModel.groupedResults.value.keys.toList())
        assertTrue(viewModel.groupedResults.value.containsKey("А"))
    }

    @Test
    fun nonEmptyQuery_buildsFlatResultSectionWithoutLetterGroups() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val hybridSearchService = mockk<HybridSearchService> {
            coEvery { performHybridSearch("дом", any()) } returns listOf(signHome)
        }
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = hybridSearchService,
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        viewModel.searchQuery.value = "дом"
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(listOf(""), viewModel.groupedResults.value.keys.toList())
        assertEquals(listOf(signHome), viewModel.groupedResults.value.getValue(""))
    }

    @Test
    fun categoryFilter_andSortToggle_updateResultsDeterministically() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = mockk(relaxed = true),
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        viewModel.selectedCategoryId.value = "category-b"
        advanceUntilIdle()

        assertEquals(listOf("Банан", "Дом"), viewModel.searchResults.value.map(Sign::word))

        viewModel.sortOrder.value = SortOrder.DESCENDING
        advanceUntilIdle()

        assertEquals(listOf("Дом", "Банан"), viewModel.searchResults.value.map(Sign::word))
        assertEquals(listOf("Б", "Д"), viewModel.groupedResults.value.keys.toList())
    }

    @Test
    fun searchFailure_mapsErrorMessage() = runTest {
        val repositoryData = syncData
        val signRepository = mockk<com.rsl.dictionary.repositories.protocols.SignRepository> {
            every {
                dataStatus
            } returns MutableStateFlow(
                RepositoryDataStatus.NoData(DataStatusReason.ServerUnavailable)
            )
            every { syncData } returns MutableStateFlow(repositoryData)
            every { refreshState } returns MutableStateFlow(
                com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState.Idle
            )
            coEvery { loadDataWithSync() } returns repositoryData
            coEvery { getAllSigns() } throws SignRepositoryError.NetworkError(IOException("offline"))
            coEvery { refresh(any()) } returns RefreshResult.NotModified(repositoryData)
        }
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = mockk(relaxed = true),
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        assertEquals("Сервер недоступен. Попробуйте позже.", viewModel.error.value)
        assertEquals(
            ScreenDataStatus.Error(DataStatusReason.ServerUnavailable),
            viewModel.screenStatus.value
        )
    }

    @Test
    fun reload_reusesCurrentFiltersAndQuery() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val hybridSearchService = mockk<HybridSearchService> {
            coEvery { performHybridSearch("д", listOf(signBanana, signHome)) } returns listOf(signHome)
        }
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = hybridSearchService,
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        viewModel.selectedCategoryId.value = "category-b"
        viewModel.searchQuery.value = "д"
        advanceTimeBy(300)
        advanceUntilIdle()

        viewModel.reload()
        advanceUntilIdle()

        coVerify(exactly = 2) {
            hybridSearchService.performHybridSearch("д", listOf(signBanana, signHome))
        }
    }

    @Test
    fun repositoryUpdate_recomputesResultsWithoutManualRefresh() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val updated = TestDataFactory.syncData(
            categories = listOf(categoryB, categoryA),
            signs = listOf(
                signBanana,
                TestDataFactory.sign(id = "sign-4", word = "Дельфин", categoryId = "category-b")
            )
        )
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = mockk(relaxed = true),
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        signRepository.replaceData(updated)
        advanceUntilIdle()

        assertEquals(listOf("Банан", "Дельфин"), viewModel.searchResults.value.map(Sign::word))
    }

    @Test
    fun cachedDataWarning_isExposedThroughScreenStatus() = runTest {
        val signRepository = FakeSignRepository(syncData)
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = mockk(relaxed = true),
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        signRepository.nextRefreshResult = RefreshResult.UsedCachedData(
            data = syncData,
            reason = com.rsl.dictionary.repositories.protocols.CachedDataReason.NO_INTERNET
        )
        signRepository.refresh(com.rsl.dictionary.repositories.protocols.RefreshReason.BACKGROUND)
        advanceUntilIdle()

        assertEquals(
            ScreenDataStatus.LoadedWithCachedWarning(DataStatusReason.NoInternet),
            viewModel.screenStatus.value
        )
        assertEquals("Нет интернета. Показаны сохранённые данные.", viewModel.statusMessage.value)
    }

    @Test
    fun retryAfterBlockingError_withNotModified_producesUpToDateStateInsteadOfError() = runTest {
        val repositoryData = syncData
        val dataStatus = MutableStateFlow<RepositoryDataStatus>(
            RepositoryDataStatus.NoData(DataStatusReason.ServerUnavailable)
        )
        val refreshStateFlow =
            MutableStateFlow<com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState>(
                com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState.Idle
            )
        val signRepository = mockk<com.rsl.dictionary.repositories.protocols.SignRepository> {
            every { this@mockk.dataStatus } returns dataStatus
            every { syncData } returns MutableStateFlow(repositoryData)
            every { refreshState } returns refreshStateFlow
            coEvery { loadDataWithSync() } returns repositoryData
            coEvery { getAllSigns() } throws SignRepositoryError.NoDataAvailable andThen repositoryData.signs
            coEvery { refresh(any()) } coAnswers {
                dataStatus.value = RepositoryDataStatus.UpToDate
                refreshStateFlow.value = com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState.Completed(
                    com.rsl.dictionary.repositories.protocols.RefreshReason.MANUAL_RETRY_AFTER_ERROR,
                    RefreshResult.NotModified(repositoryData)
                )
                RefreshResult.NotModified(repositoryData)
            }
        }
        val viewModel = SearchViewModel(
            signRepository = signRepository,
            hybridSearchService = mockk(relaxed = true),
            categoryService = CategoryService(signRepository),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        assertEquals(ScreenDataStatus.Error(DataStatusReason.ServerUnavailable), viewModel.screenStatus.value)

        viewModel.reload()
        advanceUntilIdle()

        assertEquals(ScreenDataStatus.UpToDate, viewModel.screenStatus.value)
        assertEquals("Данные актуальны", viewModel.statusMessage.value)
        assertTrue(viewModel.error.value == null)
    }
}

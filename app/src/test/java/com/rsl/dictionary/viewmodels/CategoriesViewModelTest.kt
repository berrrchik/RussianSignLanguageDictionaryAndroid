package com.rsl.dictionary.viewmodels

import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.ScreenDataStatus
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class CategoriesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    @Test
    fun init_automaticallyLoadsCategories() = runTest {
        val categories = listOf(
            TestDataFactory.category(id = "category-1", name = "Быт"),
            TestDataFactory.category(id = "category-2", name = "Учеба")
        )
        val categoryState = MutableStateFlow(emptyList<com.rsl.dictionary.models.Category>())
        val categoryService = mockCategoryService(categoryState) {
            categoryState.value = categories
            categories
        }

        val viewModel = CategoriesViewModel(
            signRepository = mockSignRepository(),
            categoryService = categoryService,
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { categoryService.getCategories() }
        assertEquals(categories, viewModel.categories.value)
        assertEquals(ScreenDataStatus.Loaded, viewModel.screenStatus.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun loadFailure_usesRepositoryNoDataStatus() = runTest {
        val categoryService = mockCategoryService(
            MutableStateFlow(emptyList())
        ) {
            throw SignRepositoryError.NetworkError(IOException("offline"))
        }

        val viewModel = CategoriesViewModel(
            signRepository = mockSignRepository(
                dataStatus = MutableStateFlow(
                    RepositoryDataStatus.NoData(DataStatusReason.ServerUnavailable)
                )
            ),
            categoryService = categoryService,
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
    fun loadCategories_triggersNewLoad() = runTest {
        val firstLoad = listOf(TestDataFactory.category(id = "category-1", name = "Первый"))
        val secondLoad = listOf(TestDataFactory.category(id = "category-2", name = "Второй"))
        val categoryState = MutableStateFlow(emptyList<com.rsl.dictionary.models.Category>())
        val categoryService = mockCategoryService(categoryState) {
            val next = if (categoryState.value.isEmpty()) firstLoad else secondLoad
            categoryState.value = next
            next
        }
        val viewModel = CategoriesViewModel(
            signRepository = mockSignRepository(),
            categoryService = categoryService,
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        viewModel.loadCategories()
        advanceUntilIdle()

        coVerify(exactly = 2) { categoryService.getCategories() }
        assertEquals(secondLoad, viewModel.categories.value)
    }

    @Test
    fun repositoryCachedWarning_updatesScreenStatus() = runTest {
        val categories = listOf(TestDataFactory.category(id = "category-1", name = "Быт"))
        val dataStatus = MutableStateFlow<RepositoryDataStatus>(RepositoryDataStatus.Idle)
        val categoryState = MutableStateFlow(emptyList<com.rsl.dictionary.models.Category>())
        val categoryService = mockCategoryService(categoryState) {
            categoryState.value = categories
            categories
        }
        val viewModel = CategoriesViewModel(
            signRepository = mockSignRepository(dataStatus = dataStatus),
            categoryService = categoryService,
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        dataStatus.value = RepositoryDataStatus.UsingCachedData(DataStatusReason.NoInternet)
        advanceUntilIdle()

        assertEquals(
            ScreenDataStatus.LoadedWithCachedWarning(DataStatusReason.NoInternet),
            viewModel.screenStatus.value
        )
        assertEquals("Нет интернета. Показаны сохранённые данные.", viewModel.statusMessage.value)
    }

    @Test
    fun onCategoryOpened_logsAnalyticsWithCategoryArguments() {
        val categoryService = mockCategoryService(
            MutableStateFlow(emptyList())
        ) { emptyList() }
        val category = TestDataFactory.category(id = "category-42", name = "Животные")
        val viewModel = CategoriesViewModel(
            signRepository = mockSignRepository(),
            categoryService = categoryService,
            analyticsService = analyticsService
        )

        viewModel.onCategoryOpened(category)

        verify(exactly = 1) {
            analyticsService.logCategoryOpened("category-42", "Животные")
        }
    }

    private fun mockCategoryService(
        categoryState: MutableStateFlow<List<com.rsl.dictionary.models.Category>>,
        provider: suspend () -> List<com.rsl.dictionary.models.Category>
    ): CategoryService {
        return mockk {
            every { categoriesFlow } returns categoryState
            coEvery { getCategories() } coAnswers { provider() }
        }
    }

    private fun mockSignRepository(
        dataStatus: MutableStateFlow<RepositoryDataStatus> = MutableStateFlow(RepositoryDataStatus.Idle)
    ): SignRepository {
        return mockk {
            every { this@mockk.dataStatus } returns dataStatus
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
        }
    }
}

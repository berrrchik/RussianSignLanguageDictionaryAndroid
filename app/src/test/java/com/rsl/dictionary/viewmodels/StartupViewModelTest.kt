package com.rsl.dictionary.viewmodels

import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.RefreshResult
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
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    @Test
    fun init_refreshesThroughRepositoryThenPreloadsData() = runTest {
        val order = mutableListOf<String>()
        val data = TestDataFactory.syncData(lastUpdated = 321L)
        val signRepository = mockSignRepository(
            dataStatus = MutableStateFlow(RepositoryDataStatus.Updated)
        ) {
            coEvery { refresh(RefreshReason.STARTUP) } coAnswers {
                order += "refresh"
                RefreshResult.Updated(data)
            }
            coEvery { getAllSigns() } coAnswers {
                order += "signs"
                data.signs
            }
        }
        val categoryService = mockk<CategoryService> {
            coEvery { getCategories() } coAnswers {
                order += "categories"
                data.categories
            }
        }

        StartupViewModel(
            signRepository = signRepository,
            categoryService = categoryService,
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        assertEquals(listOf("refresh", "signs", "categories"), order)
        verify(exactly = 1) { analyticsService.logSyncCompleted() }
    }

    @Test
    fun init_withCachedDataFallbackStillLoadsLocalData() = runTest {
        val data = TestDataFactory.syncData()
        val signRepository = mockSignRepository(
            dataStatus = MutableStateFlow(
                RepositoryDataStatus.UsingCachedData(DataStatusReason.NoInternet)
            )
        ) {
            coEvery {
                refresh(RefreshReason.STARTUP)
            } returns RefreshResult.UsedCachedData(
                data,
                reason = com.rsl.dictionary.repositories.protocols.CachedDataReason.NO_INTERNET
            )
            coEvery { getAllSigns() } returns data.signs
        }
        val categoryService = mockk<CategoryService> {
            coEvery { getCategories() } returns data.categories
        }

        StartupViewModel(
            signRepository = signRepository,
            categoryService = categoryService,
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { signRepository.refresh(RefreshReason.STARTUP) }
        coVerify(exactly = 1) { signRepository.getAllSigns() }
        coVerify(exactly = 1) { categoryService.getCategories() }
    }

    @Test
    fun startupFailure_mapsIntoFirstLaunchMessage() = runTest {
        val viewModel = StartupViewModel(
            signRepository = mockSignRepository(
                dataStatus = MutableStateFlow(
                    RepositoryDataStatus.NoData(DataStatusReason.NoInternet)
                )
            ) {
                coEvery { refresh(RefreshReason.STARTUP) } returns RefreshResult.NoInternet
            },
            categoryService = mockk(relaxed = true),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        assertEquals("Для первого запуска требуется интернет", viewModel.startupError.value)
    }

    @Test
    fun retry_whilePreparationIsRunning_doesNotStartSecondFlow() = runTest {
        val calls = AtomicInteger(0)
        val release = CompletableDeferred<Unit>()
        val signRepository = mockSignRepository(
            dataStatus = MutableStateFlow(RepositoryDataStatus.UpToDate)
        ) {
            coEvery { refresh(RefreshReason.STARTUP) } returns RefreshResult.NotModified(
                TestDataFactory.syncData()
            )
            coEvery { getAllSigns() } coAnswers {
                calls.incrementAndGet()
                release.await()
                emptyList()
            }
        }
        val viewModel = StartupViewModel(
            signRepository = signRepository,
            categoryService = mockk {
                coEvery { getCategories() } returns emptyList()
            },
            analyticsService = analyticsService
        )
        runCurrent()

        viewModel.retry()
        runCurrent()

        assertEquals(1, calls.get())

        release.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun clearError_resetsStartupError() = runTest {
        val viewModel = StartupViewModel(
            signRepository = mockSignRepository(
                dataStatus = MutableStateFlow(
                    RepositoryDataStatus.NoData(DataStatusReason.ServerUnavailable)
                )
            ) {
                coEvery { refresh(RefreshReason.STARTUP) } returns RefreshResult.ServerUnavailable
            },
            categoryService = mockk(relaxed = true),
            analyticsService = analyticsService
        )
        advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.startupError.value)
    }

    private fun mockSignRepository(
        dataStatus: MutableStateFlow<RepositoryDataStatus>,
        stub: SignRepository.() -> Unit
    ): SignRepository {
        return mockk<SignRepository>(relaxed = true).apply {
            every { this@apply.dataStatus } returns dataStatus
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
            stub()
        }
    }
}

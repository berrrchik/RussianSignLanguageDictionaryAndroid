package com.rsl.dictionary.viewmodels

import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.fakes.FakeSignRepository
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
class SyncViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    @Test
    fun sync_withUpdates_delegatesToRepositoryRefresh() = runTest {
        val data = TestDataFactory.syncData(lastUpdated = 999L)
        val signRepository = FakeSignRepository(data).apply {
            nextRefreshResult = RefreshResult.Updated(data)
        }
        val viewModel = SyncViewModel(
            signRepository = signRepository,
            analyticsService = analyticsService
        )

        viewModel.sync()
        advanceUntilIdle()

        assertEquals(listOf(RefreshReason.MANUAL_RETRY_AFTER_ERROR), signRepository.refreshCalls)
        assertEquals(
            SignRepositoryRefreshState.Completed(
                RefreshReason.MANUAL_RETRY_AFTER_ERROR,
                RefreshResult.Updated(data)
            ),
            viewModel.syncStatus.value
        )
        verify(exactly = 1) { analyticsService.logSyncCompleted() }
        assertNull(viewModel.syncError.value)
        assertEquals("Данные обновлены", viewModel.syncMessage.value)
    }

    @Test
    fun sync_withoutUpdates_stillExposesCompletedStatus() = runTest {
        val data = TestDataFactory.syncData(lastUpdated = 555L)
        val signRepository = FakeSignRepository(data).apply {
            nextRefreshResult = RefreshResult.NotModified(data)
        }
        val viewModel = SyncViewModel(
            signRepository = signRepository,
            analyticsService = analyticsService
        )

        viewModel.sync()
        advanceUntilIdle()

        assertEquals(
            SignRepositoryRefreshState.Completed(
                RefreshReason.MANUAL_RETRY_AFTER_ERROR,
                RefreshResult.NotModified(data)
            ),
            viewModel.syncStatus.value
        )
        assertNull(viewModel.syncError.value)
        assertEquals("Данные актуальны", viewModel.syncMessage.value)
    }

    @Test
    fun offlineSync_mapsIntoSyncErrorState() = runTest {
        val signRepository = FakeSignRepository().apply {
            nextRefreshResult = RefreshResult.NoInternet
        }
        val viewModel = SyncViewModel(
            signRepository = signRepository,
            analyticsService = analyticsService
        )

        viewModel.sync()
        advanceUntilIdle()

        assertEquals("Нет подключения к интернету", viewModel.syncError.value)
        verify { analyticsService.logSyncFailed("no_internet") }
    }

    @Test
    fun syncError_mapsIntoSyncErrorState() = runTest {
        val signRepository = mockk<SignRepository> {
            every { dataStatus } returns MutableStateFlow(com.rsl.dictionary.models.RepositoryDataStatus.Idle)
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
            coEvery {
                refresh(RefreshReason.MANUAL_RETRY_AFTER_ERROR)
            } returns RefreshResult.NetworkError(IOException("offline"))
        }
        val viewModel = SyncViewModel(
            signRepository = signRepository,
            analyticsService = analyticsService
        )

        viewModel.sync()
        advanceUntilIdle()

        assertEquals("Ошибка сети", viewModel.syncError.value)
        verify { analyticsService.logSyncFailed("network_error") }
    }

    @Test
    fun clearError_resetsSyncErrorState() = runTest {
        val signRepository = FakeSignRepository().apply {
            nextRefreshResult = RefreshResult.ServerUnavailable
        }
        val viewModel = SyncViewModel(
            signRepository = signRepository,
            analyticsService = analyticsService
        )
        viewModel.sync()
        advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.syncError.value)
    }

    @Test
    fun repeatedSyncWhileRepositoryIsRefreshing_doesNotStartNewWork() = runTest {
        val signRepository = mockk<SignRepository> {
            every { dataStatus } returns MutableStateFlow(com.rsl.dictionary.models.RepositoryDataStatus.Idle)
            every { syncData } returns MutableStateFlow(null)
            every {
                refreshState
            } returns MutableStateFlow(
                SignRepositoryRefreshState.Refreshing(RefreshReason.BACKGROUND)
            )
        }
        val viewModel = SyncViewModel(
            signRepository = signRepository,
            analyticsService = analyticsService
        )

        viewModel.sync()
        advanceUntilIdle()

        coVerify(exactly = 0) { signRepository.refresh(any()) }
    }
}

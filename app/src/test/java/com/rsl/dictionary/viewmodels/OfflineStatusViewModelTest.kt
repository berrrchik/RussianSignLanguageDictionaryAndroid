package com.rsl.dictionary.viewmodels

import app.cash.turbine.test
import com.rsl.dictionary.models.ConnectivityStatus
import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.OfflineIndicatorStatus
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineStatusViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialState_matchesConnectivityAndRepositoryStatus() {
        val connectivity = MutableStateFlow(false)
        val dataStatus = MutableStateFlow<RepositoryDataStatus>(RepositoryDataStatus.Idle)
        val viewModel = OfflineStatusViewModel(
            networkMonitor = mockk<NetworkMonitor> {
                every { isConnectedFlow } returns connectivity
                every { isConnected() } returns false
            },
            signRepository = mockSignRepository(dataStatus)
        )

        assertEquals(ConnectivityStatus.NoInternet, viewModel.connectivityStatus.value)
        assertEquals(OfflineIndicatorStatus.NoInternet, viewModel.indicatorStatus.value)
    }

    @Test
    fun connectivityAndCachedServerFailure_areExposedSeparately() = runTest {
        val connectivity = MutableStateFlow(true)
        val dataStatus = MutableStateFlow<RepositoryDataStatus>(RepositoryDataStatus.Idle)
        val viewModel = OfflineStatusViewModel(
            networkMonitor = mockk<NetworkMonitor> {
                every { isConnectedFlow } returns connectivity
                every { isConnected() } returns true
            },
            signRepository = mockSignRepository(dataStatus)
        )

        viewModel.indicatorStatus.test {
            assertNull(awaitItem())

            dataStatus.value = RepositoryDataStatus.UsingCachedData(DataStatusReason.ServerUnavailable)
            assertEquals(
                OfflineIndicatorStatus.UsingCachedData(DataStatusReason.ServerUnavailable),
                awaitItem()
            )

            connectivity.value = false
            expectNoEvents()
        }
    }

    @Test
    fun noDataStatus_usesRepositoryClassificationInsteadOfReachabilityGuess() = runTest {
        val connectivity = MutableStateFlow(true)
        val dataStatus = MutableStateFlow<RepositoryDataStatus>(
            RepositoryDataStatus.NoData(DataStatusReason.ServerUnavailable)
        )
        val viewModel = OfflineStatusViewModel(
            networkMonitor = mockk<NetworkMonitor> {
                every { isConnectedFlow } returns connectivity
                every { isConnected() } returns true
            },
            signRepository = mockSignRepository(dataStatus)
        )

        assertEquals(
            OfflineIndicatorStatus.NoData(DataStatusReason.ServerUnavailable),
            viewModel.indicatorStatus.value
        )
    }

    private fun mockSignRepository(
        dataStatus: MutableStateFlow<RepositoryDataStatus>
    ): SignRepository {
        return mockk {
            every { this@mockk.dataStatus } returns dataStatus
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
        }
    }
}

package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.ConnectivityStatus
import com.rsl.dictionary.models.OfflineIndicatorStatus
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.services.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class OfflineStatusViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    signRepository: SignRepository
) : ViewModel() {
    val connectivityStatus: StateFlow<ConnectivityStatus> = networkMonitor.isConnectedFlow
        .map { isConnected ->
            if (isConnected) ConnectivityStatus.Connected else ConnectivityStatus.NoInternet
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = if (networkMonitor.isConnected()) {
                ConnectivityStatus.Connected
            } else {
                ConnectivityStatus.NoInternet
            }
        )

    val dataStatus: StateFlow<RepositoryDataStatus> = signRepository.dataStatus

    val indicatorStatus: StateFlow<OfflineIndicatorStatus?> = combine(
        connectivityStatus,
        signRepository.dataStatus
    ) { connectivityStatus, dataStatus ->
        when (dataStatus) {
            is RepositoryDataStatus.UsingCachedData -> OfflineIndicatorStatus.UsingCachedData(
                dataStatus.reason
            )

            is RepositoryDataStatus.NoData -> OfflineIndicatorStatus.NoData(dataStatus.reason)
            RepositoryDataStatus.Idle -> {
                if (connectivityStatus == ConnectivityStatus.NoInternet) {
                    OfflineIndicatorStatus.NoInternet
                } else {
                    null
                }
            }

            RepositoryDataStatus.Updated,
            RepositoryDataStatus.UpToDate -> {
                if (connectivityStatus == ConnectivityStatus.NoInternet) {
                    OfflineIndicatorStatus.NoInternet
                } else {
                    null
                }
            }
        }
    }.distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialIndicatorStatus(
                connectivityStatus = if (networkMonitor.isConnected()) {
                    ConnectivityStatus.Connected
                } else {
                    ConnectivityStatus.NoInternet
                },
                dataStatus = signRepository.dataStatus.value
            )
        )

    private fun initialIndicatorStatus(
        connectivityStatus: ConnectivityStatus,
        dataStatus: RepositoryDataStatus
    ): OfflineIndicatorStatus? = when (dataStatus) {
        is RepositoryDataStatus.UsingCachedData -> OfflineIndicatorStatus.UsingCachedData(
            dataStatus.reason
        )

        is RepositoryDataStatus.NoData -> OfflineIndicatorStatus.NoData(dataStatus.reason)
        RepositoryDataStatus.Idle,
        RepositoryDataStatus.Updated,
        RepositoryDataStatus.UpToDate -> {
            if (connectivityStatus == ConnectivityStatus.NoInternet) {
                OfflineIndicatorStatus.NoInternet
            } else {
                null
            }
        }
    }
}

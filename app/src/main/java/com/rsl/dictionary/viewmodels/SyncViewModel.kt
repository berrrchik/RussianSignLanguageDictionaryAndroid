package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val signRepository: SignRepository,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(
        signRepository.refreshState.value is SignRepositoryRefreshState.Refreshing
    )
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val syncStatus: StateFlow<SignRepositoryRefreshState> = signRepository.refreshState

    init {
        viewModelScope.launch {
            signRepository.refreshState.collect { state ->
                _isSyncing.value = state is SignRepositoryRefreshState.Refreshing
            }
        }
    }

    fun sync() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _syncError.value = null
            _syncMessage.value = null

            try {
                val result = signRepository.refresh(RefreshReason.MANUAL_RETRY_AFTER_ERROR)
                handleSyncResult(result)
            } catch (error: Exception) {
                Timber.e(error, "Unexpected sync failure")
                analyticsService.logSyncFailed("unknown_error")
                _syncError.value = ErrorMessageMapper.map(error)
            }
        }
    }

    fun clearError() {
        _syncError.value = null
    }

    private fun handleSyncResult(result: RefreshResult) {
        when (result) {
            is RefreshResult.Updated -> {
                analyticsService.logSyncCompleted()
                _syncMessage.value = ErrorMessageMapper.map(RepositoryDataStatus.Updated)
            }

            is RefreshResult.NotModified -> {
                analyticsService.logSyncCompleted()
                _syncMessage.value = ErrorMessageMapper.map(RepositoryDataStatus.UpToDate)
            }

            is RefreshResult.NoInternet -> {
                analyticsService.logSyncFailed("no_internet")
                _syncError.value = ErrorMessageMapper.map(SyncError.NoInternet)
            }

            is RefreshResult.ServerUnavailable -> {
                analyticsService.logSyncFailed("server_unavailable")
                _syncError.value = ErrorMessageMapper.map(SyncError.ServerUnavailable)
            }

            is RefreshResult.NetworkError -> {
                analyticsService.logSyncFailed("network_error")
                _syncError.value = ErrorMessageMapper.map(SyncError.NetworkError(result.cause))
            }

            is RefreshResult.DecodingError -> {
                analyticsService.logSyncFailed("decoding_error")
                _syncError.value = ErrorMessageMapper.map(SyncError.DecodingError(result.cause))
            }

            is RefreshResult.UnknownError -> {
                analyticsService.logSyncFailed("unknown_error")
                _syncError.value = ErrorMessageMapper.map(SyncError.UnknownError(result.cause))
            }

            is RefreshResult.UsedCachedData -> {
                _syncMessage.value = ErrorMessageMapper.map(
                    RepositoryDataStatus.UsingCachedData(
                        if (result.reason == com.rsl.dictionary.repositories.protocols.CachedDataReason.NO_INTERNET) {
                            com.rsl.dictionary.models.DataStatusReason.NoInternet
                        } else {
                            com.rsl.dictionary.models.DataStatusReason.ServerUnavailable
                        }
                    )
                )
            }
        }
    }
}

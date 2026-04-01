package com.rsl.dictionary.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.cache.CacheService
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

sealed class SyncEvent {
    object CategoriesUpdated : SyncEvent()
    object SignsUpdated : SyncEvent()
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val cacheService: CacheService,
    private val networkMonitor: NetworkMonitor,
    private val sharedPreferences: SharedPreferences,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _syncEvents = MutableSharedFlow<SyncEvent>()
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()

    fun sync() {
        viewModelScope.launch {
            if (!networkMonitor.isConnected()) return@launch

            _isSyncing.value = true
            _syncError.value = null

            try {
                val lastSyncDate = sharedPreferences.getLong(LAST_SYNC_DATE_KEY, 0L)
                val metadata = syncRepository.checkForUpdates(lastSyncDate)
                if (!metadata.hasUpdates) return@launch

                val data = syncRepository.fetchAllData {
                    runBlocking { cacheService.load() }
                }
                cacheService.save(data)
                sharedPreferences.edit().putLong(LAST_SYNC_DATE_KEY, data.lastUpdated).apply()
                _syncEvents.emit(SyncEvent.CategoriesUpdated)
                _syncEvents.emit(SyncEvent.SignsUpdated)
                analyticsService.logSyncCompleted()
            } catch (error: SyncError) {
                Timber.e(error, "Sync failed")
                analyticsService.logSyncFailed(syncFailureReason(error))
                _syncError.value = ErrorMessageMapper.map(error)
            } catch (error: Exception) {
                Timber.e(error, "Unexpected sync failure")
                analyticsService.logSyncFailed(error.message ?: "unknown_error")
                _syncError.value = ErrorMessageMapper.map(error)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearError() {
        _syncError.value = null
    }

    private companion object {
        const val LAST_SYNC_DATE_KEY = "lastSyncDate"
    }

    private fun syncFailureReason(error: SyncError): String = when (error) {
        is SyncError.NoInternet -> "no_internet"
        is SyncError.ServerUnavailable -> "server_unavailable"
        is SyncError.NetworkError -> "network_error"
        is SyncError.DecodingError -> "decoding_error"
        is SyncError.UnknownError -> "unknown_error"
    }
}

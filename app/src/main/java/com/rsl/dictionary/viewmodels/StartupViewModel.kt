package com.rsl.dictionary.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.cache.CacheService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val signRepository: SignRepository,
    private val categoryService: CategoryService,
    private val cacheService: CacheService,
    private val networkMonitor: NetworkMonitor,
    private val sharedPreferences: SharedPreferences,
    private val analyticsService: AnalyticsService
) : ViewModel() {
    private var hasStarted = false

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _startupError = MutableStateFlow<String?>(null)
    val startupError: StateFlow<String?> = _startupError.asStateFlow()

    init {
        startIfNeeded()
    }

    fun clearError() {
        _startupError.value = null
    }

    fun retry() {
        launchPreparation()
    }

    private fun startIfNeeded() {
        if (hasStarted) return
        hasStarted = true
        launchPreparation()
    }

    private fun launchPreparation() {
        if (_isPreparing.value) return

        _isPreparing.value = true

        viewModelScope.launch {
            _startupError.value = null

            try {
                syncIfNeeded()
                signRepository.getAllSigns()
                categoryService.getCategories()
            } catch (error: SyncError) {
                Timber.e(error, "Startup sync failed")
                analyticsService.logSyncFailed(syncFailureReason(error))
                _startupError.value = ErrorMessageMapper.map(error)
            } catch (error: Exception) {
                Timber.e(error, "Startup preparation failed")
                analyticsService.logSyncFailed(error.message ?: "unknown_error")
                _startupError.value = ErrorMessageMapper.map(error)
            } finally {
                _isPreparing.value = false
            }
        }
    }

    private suspend fun syncIfNeeded() {
        if (!networkMonitor.isConnected()) return

        val lastSyncDate = sharedPreferences.getLong(LAST_SYNC_DATE_KEY, 0L)
        val metadata = syncRepository.checkForUpdates(lastSyncDate)
        if (!metadata.hasUpdates) return

        val data = syncRepository.fetchAllData {
            runBlocking { cacheService.load() }
        }
        cacheService.save(data)
        sharedPreferences.edit().putLong(LAST_SYNC_DATE_KEY, data.lastUpdated).apply()
        analyticsService.logSyncCompleted()
    }

    private fun syncFailureReason(error: SyncError): String = when (error) {
        is SyncError.NoInternet -> "no_internet"
        is SyncError.ServerUnavailable -> "server_unavailable"
        is SyncError.NetworkError -> "network_error"
        is SyncError.DecodingError -> "decoding_error"
        is SyncError.UnknownError -> "unknown_error"
    }

    private companion object {
        const val LAST_SYNC_DATE_KEY = "lastSyncDate"
    }
}

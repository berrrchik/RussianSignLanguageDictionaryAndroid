package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.RefreshResult
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val signRepository: SignRepository,
    private val categoryService: CategoryService,
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
                val refreshResult = signRepository.refresh(RefreshReason.STARTUP)
                logRefreshOutcome(refreshResult)
                refreshResult.toFailureOrNull()?.let { throw it }
                signRepository.getAllSigns()
                categoryService.getCategories()
            } catch (error: Exception) {
                Timber.e(error, "Startup preparation failed")
                startupFailureReason(error)?.let(analyticsService::logSyncFailed)
                _startupError.value = ErrorMessageMapper.map(signRepository.dataStatus.value)
                    ?: ErrorMessageMapper.map(error)
            } finally {
                _isPreparing.value = false
            }
        }
    }

    private fun logRefreshOutcome(result: RefreshResult) {
        when (result) {
            is RefreshResult.Updated,
            is RefreshResult.NotModified -> analyticsService.logSyncCompleted()

            else -> Unit
        }
    }

    private fun RefreshResult.toFailureOrNull(): Throwable? = when (this) {
        is RefreshResult.NoInternet -> SyncError.NoInternet
        is RefreshResult.ServerUnavailable -> SyncError.ServerUnavailable
        is RefreshResult.NetworkError -> SyncError.NetworkError(cause)
        is RefreshResult.DecodingError -> SyncError.DecodingError(cause)
        is RefreshResult.UnknownError -> SyncError.UnknownError(cause)
        else -> null
    }

    private fun startupFailureReason(error: Throwable): String? = when (error) {
        is SyncError.NoInternet -> "no_internet"
        is SyncError.ServerUnavailable -> "server_unavailable"
        is SyncError.NetworkError -> "network_error"
        is SyncError.DecodingError -> "decoding_error"
        is SyncError.UnknownError -> "unknown_error"
        else -> "unknown_error"
    }
}

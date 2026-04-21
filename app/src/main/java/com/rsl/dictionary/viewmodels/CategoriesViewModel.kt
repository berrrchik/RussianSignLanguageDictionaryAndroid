package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.ScreenDataStatus
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.models.Category
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val signRepository: SignRepository,
    private val categoryService: CategoryService,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _screenStatus = MutableStateFlow<ScreenDataStatus>(ScreenDataStatus.Loaded)
    val screenStatus: StateFlow<ScreenDataStatus> = _screenStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var latestDataStatus: RepositoryDataStatus = signRepository.dataStatus.value
    private var latestRefreshState: SignRepositoryRefreshState = signRepository.refreshState.value
    private var hasLoadedData = false

    init {
        viewModelScope.launch {
            combine(
                signRepository.dataStatus,
                signRepository.refreshState
            ) { dataStatus, refreshState ->
                dataStatus to refreshState
            }.collect { (dataStatus, refreshState) ->
                latestDataStatus = dataStatus
                latestRefreshState = refreshState
                updateScreenStatus()
            }
        }

        viewModelScope.launch {
            categoryService.categoriesFlow.collect { categories ->
                _categories.value = categories
            }
        }
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { categoryService.getCategories() }
                .onSuccess {
                    hasLoadedData = true
                    _categories.value = it
                    updateScreenStatus()
                }
                .onFailure {
                    updateScreenStatus()
                    _error.value = _statusMessage.value ?: ErrorMessageMapper.map(it)
                }
            _isLoading.value = false
        }
    }

    fun retryAfterBlockingError() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            signRepository.refresh(RefreshReason.MANUAL_RETRY_AFTER_ERROR)
            runCatching { categoryService.getCategories() }
                .onSuccess {
                    hasLoadedData = true
                    _categories.value = it
                    updateScreenStatus()
                }
                .onFailure {
                    updateScreenStatus()
                    _error.value = _statusMessage.value ?: ErrorMessageMapper.map(it)
                }
            _isLoading.value = false
        }
    }

    fun onCategoryOpened(category: Category) {
        analyticsService.logCategoryOpened(category.id, category.name)
    }

    private fun updateScreenStatus() {
        val dataStatus = latestDataStatus
        val refreshState = latestRefreshState
        val status = when (dataStatus) {
            is RepositoryDataStatus.UsingCachedData -> {
                ScreenDataStatus.LoadedWithCachedWarning(dataStatus.reason)
            }

            is RepositoryDataStatus.NoData -> {
                if (hasLoadedData) {
                    ScreenDataStatus.Loaded
                } else {
                    ScreenDataStatus.Error(dataStatus.reason)
                }
            }

            RepositoryDataStatus.Updated -> {
                if (refreshState is SignRepositoryRefreshState.Completed &&
                    refreshState.reason == RefreshReason.MANUAL_RETRY_AFTER_ERROR
                ) {
                    ScreenDataStatus.Updated
                } else {
                    ScreenDataStatus.Loaded
                }
            }

            RepositoryDataStatus.UpToDate -> {
                if (refreshState is SignRepositoryRefreshState.Completed &&
                    refreshState.reason == RefreshReason.MANUAL_RETRY_AFTER_ERROR
                ) {
                    ScreenDataStatus.UpToDate
                } else {
                    ScreenDataStatus.Loaded
                }
            }

            RepositoryDataStatus.Idle -> ScreenDataStatus.Loaded
        }

        _screenStatus.value = status
        _statusMessage.value = ErrorMessageMapper.map(status)
        if (status !is ScreenDataStatus.Error && _error.value == _statusMessage.value) {
            _error.value = null
        }
    }
}

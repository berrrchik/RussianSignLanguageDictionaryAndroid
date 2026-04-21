package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.ScreenDataStatus
import com.rsl.dictionary.models.Category
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.RefreshReason
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.category.CategoryService
import com.rsl.dictionary.services.search.HybridSearchService
import com.rsl.dictionary.utilities.ErrorMessageMapper
import com.rsl.dictionary.utilities.data.SignGroupingHelper.groupByFirstLetter
import com.rsl.dictionary.utilities.data.SignGroupingHelper.sortSignsAlphabetically
import com.rsl.dictionary.utilities.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val signRepository: SignRepository,
    private val hybridSearchService: HybridSearchService,
    private val categoryService: CategoryService,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<String?>(null)
    val sortOrder = MutableStateFlow(SortOrder.ASCENDING)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _screenStatus = MutableStateFlow<ScreenDataStatus>(ScreenDataStatus.Loaded)
    val screenStatus: StateFlow<ScreenDataStatus> = _screenStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Sign>>(emptyList())
    val searchResults: StateFlow<List<Sign>> = _searchResults.asStateFlow()

    private val _groupedResults = MutableStateFlow<Map<String, List<Sign>>>(emptyMap())
    val groupedResults: StateFlow<Map<String, List<Sign>>> = _groupedResults.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryService.categoriesFlow

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
            runCatching { categoryService.getCategories() }
        }

        viewModelScope.launch {
            performSearch(
                query = searchQuery.value,
                categoryId = selectedCategoryId.value,
                order = sortOrder.value,
                trackAnalytics = true
            )
        }

        viewModelScope.launch {
            var isFirstEmission = true
            combine(
                searchQuery.debounce(300L),
                selectedCategoryId,
                sortOrder
            ) { query, categoryId, order ->
                Triple(query, categoryId, order)
            }.collect { (query, categoryId, order) ->
                if (isFirstEmission) {
                    isFirstEmission = false
                    return@collect
                }
                performSearch(query, categoryId, order, trackAnalytics = true)
            }
        }

        viewModelScope.launch {
            signRepository.syncData
                .filterNotNull()
                .drop(1)
                .collect { syncData ->
                    performSearch(
                        query = searchQuery.value,
                        categoryId = selectedCategoryId.value,
                        order = sortOrder.value,
                        availableSigns = syncData.signs,
                        trackAnalytics = false
                    )
                }
        }
    }

    private suspend fun performSearch(
        query: String,
        categoryId: String?,
        order: SortOrder,
        availableSigns: List<Sign>? = null,
        trackAnalytics: Boolean
    ) {
        _isLoading.value = true
        _error.value = null

        runCatching {
            val allSigns = availableSigns ?: signRepository.getAllSigns()
            val filteredByCategory = allSigns.filterByCategory(categoryId)

            if (query.isBlank()) {
                sortAlphabetically(filteredByCategory, order)
            } else {
                hybridSearchService.performHybridSearch(query, filteredByCategory)
            }
        }.onSuccess { results ->
            hasLoadedData = true
            val searchType = if (query.isBlank()) "local" else "hybrid"
            if (trackAnalytics) {
                analyticsService.logSearchPerformed(query, results.size, searchType)
            }
            _searchResults.value = results
            _groupedResults.value = if (query.isBlank()) {
                groupByFirstLetter(sortAlphabetically(results, order), order)
            } else if (results.isEmpty()) {
                emptyMap()
            } else {
                linkedMapOf("" to results)
            }
            updateScreenStatus()
        }.onFailure { throwable ->
            Timber.e(throwable, "Search failed for query=%s", query)
            if (!hasLoadedData) {
                updateScreenStatus()
                _error.value = _statusMessage.value ?: ErrorMessageMapper.map(throwable)
            } else {
                _error.value = ErrorMessageMapper.map(throwable)
            }
        }

        _isLoading.value = false
    }

    private fun List<Sign>.filterByCategory(categoryId: String?): List<Sign> {
        return if (categoryId.isNullOrBlank()) this else filter { it.categoryId == categoryId }
    }

    private fun sortAlphabetically(signs: List<Sign>, order: SortOrder): List<Sign> =
        sortSignsAlphabetically(signs, order)

    fun reload() {
        viewModelScope.launch {
            _error.value = null
            signRepository.refresh(RefreshReason.MANUAL_RETRY_AFTER_ERROR)
            performSearch(
                query = searchQuery.value,
                categoryId = selectedCategoryId.value,
                order = sortOrder.value,
                trackAnalytics = false
            )
        }
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

package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.Category
import com.rsl.dictionary.models.Sign
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

    private val _searchResults = MutableStateFlow<List<Sign>>(emptyList())
    val searchResults: StateFlow<List<Sign>> = _searchResults.asStateFlow()

    private val _groupedResults = MutableStateFlow<Map<String, List<Sign>>>(emptyMap())
    val groupedResults: StateFlow<Map<String, List<Sign>>> = _groupedResults.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryService.categoriesFlow

    init {
        viewModelScope.launch {
            runCatching { categoryService.getCategories() }
        }

        viewModelScope.launch {
            performSearch(
                query = searchQuery.value,
                categoryId = selectedCategoryId.value,
                order = sortOrder.value
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
                performSearch(query, categoryId, order)
            }
        }
    }

    private suspend fun performSearch(
        query: String,
        categoryId: String?,
        order: SortOrder
    ) {
        _isLoading.value = true
        _error.value = null

        runCatching {
            val allSigns = signRepository.getAllSigns()
            val filteredByCategory = allSigns.filterByCategory(categoryId)

            if (query.isBlank()) {
                sortAlphabetically(filteredByCategory, order)
            } else {
                hybridSearchService.performHybridSearch(query, filteredByCategory)
            }
        }.onSuccess { results ->
            val searchType = if (query.isBlank()) "local" else "hybrid"
            analyticsService.logSearchPerformed(query, results.size, searchType)
            _searchResults.value = results
            _groupedResults.value = if (query.isBlank()) {
                groupByFirstLetter(sortAlphabetically(results, order), order)
            } else if (results.isEmpty()) {
                emptyMap()
            } else {
                linkedMapOf("" to results)
            }
        }.onFailure { throwable ->
            Timber.e(throwable, "Search failed for query=%s", query)
            _error.value = ErrorMessageMapper.map(throwable)
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
            performSearch(
                query = searchQuery.value,
                categoryId = selectedCategoryId.value,
                order = sortOrder.value
            )
        }
    }
}

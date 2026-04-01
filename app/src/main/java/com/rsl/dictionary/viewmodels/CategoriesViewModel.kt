package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.Category
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.services.category.CategoryService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryService: CategoryService,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { categoryService.getCategories() }
                .onSuccess { _categories.value = it }
                .onFailure { _error.value = it.message ?: "Failed to load categories" }
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadCategories()
    }

    fun onCategoryOpened(category: Category) {
        analyticsService.logCategoryOpened(category.id, category.name)
    }
}

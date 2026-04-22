package com.rsl.dictionary.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.utilities.ErrorMessageMapper
import com.rsl.dictionary.utilities.data.SignGroupingHelper
import com.rsl.dictionary.utilities.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val signRepository: SignRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val currentCategoryId = MutableStateFlow<String?>(null)
    private var loadDataJob: Job? = null

    private val _signs = MutableStateFlow<List<Sign>>(emptyList())
    val signs: StateFlow<List<Sign>> = _signs.asStateFlow()

    private val _groupedSigns = MutableStateFlow<Map<String, List<Sign>>>(emptyMap())
    val groupedSigns: StateFlow<Map<String, List<Sign>>> = _groupedSigns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                currentCategoryId.filterNotNull(),
                signRepository.syncData.filterNotNull()
            ) { categoryId, syncData ->
                syncData.signs.filter { it.categoryId == categoryId }
            }.collect { categorySigns ->
                _signs.value = categorySigns
                _groupedSigns.value = SignGroupingHelper.groupByFirstLetter(
                    SignGroupingHelper.sortSignsAlphabetically(
                        categorySigns,
                        SortOrder.ASCENDING
                    ),
                    SortOrder.ASCENDING
                )
                _error.value = null
                _isLoading.value = false
            }
        }

        savedStateHandle.get<String>("categoryId")?.let { categoryId ->
            loadSigns(categoryId)
        }
    }

    fun loadSigns(categoryId: String) {
        savedStateHandle["categoryId"] = categoryId
        currentCategoryId.value = categoryId
        _error.value = null

        if (signRepository.syncData.value != null) {
            _isLoading.value = false
            return
        }

        if (loadDataJob?.isActive == true) return

        _isLoading.value = true
        loadDataJob = viewModelScope.launch {
            runCatching { signRepository.loadDataWithSync() }
                .onFailure { throwable ->
                    if (signRepository.syncData.value == null) {
                        _signs.value = emptyList()
                        _groupedSigns.value = emptyMap()
                        _error.value = ErrorMessageMapper.map(throwable)
                        _isLoading.value = false
                    }
                }
        }
    }
}

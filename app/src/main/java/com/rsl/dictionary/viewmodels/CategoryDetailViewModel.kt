package com.rsl.dictionary.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.utilities.data.SignGroupingHelper
import com.rsl.dictionary.utilities.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val signRepository: SignRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _signs = MutableStateFlow<List<Sign>>(emptyList())
    val signs: StateFlow<List<Sign>> = _signs.asStateFlow()

    private val _groupedSigns = MutableStateFlow<Map<String, List<Sign>>>(emptyMap())
    val groupedSigns: StateFlow<Map<String, List<Sign>>> = _groupedSigns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        savedStateHandle.get<String>("categoryId")?.let { categoryId ->
            loadSigns(categoryId)
        }
    }

    fun loadSigns(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { signRepository.getSignsByCategory(categoryId) }
                .onSuccess { loadedSigns ->
                    _signs.value = loadedSigns
                    _groupedSigns.value = SignGroupingHelper.groupByFirstLetter(
                        SignGroupingHelper.sortSignsAlphabetically(
                            loadedSigns,
                            SortOrder.ASCENDING
                        ),
                        SortOrder.ASCENDING
                    )
                }
                .onFailure { _error.value = it.message ?: "Failed to load signs" }
            _isLoading.value = false
        }
    }
}

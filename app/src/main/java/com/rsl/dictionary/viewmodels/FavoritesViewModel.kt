package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.utilities.data.SignGroupingHelper
import com.rsl.dictionary.utilities.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val signRepository: SignRepository,
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Sign>>(emptyList())
    val favorites: StateFlow<List<Sign>> = _favorites.asStateFlow()

    private val _groupedFavorites = MutableStateFlow<Map<String, List<Sign>>>(emptyMap())
    val groupedFavorites: StateFlow<Map<String, List<Sign>>> = _groupedFavorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favoritesFlow.collect { favoriteIds ->
                loadFavoriteSigns(favoriteIds)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                favorites.value.forEach { sign ->
                    videoRepository.clearFavoritesCache(sign)
                }
                favoritesRepository.clearAll()
            }.onFailure {
                _error.value = it.message ?: "Failed to clear favorites"
            }
            _isLoading.value = false
        }
    }

    private suspend fun loadFavoriteSigns(favoriteIds: List<String>) {
        _isLoading.value = true
        _error.value = null

        runCatching {
            val allSignsById = signRepository.getAllSigns().associateBy { it.id }
            favoriteIds.mapNotNull { allSignsById[it] }
        }.onSuccess { loadedFavorites ->
            _favorites.value = loadedFavorites
            _groupedFavorites.value = SignGroupingHelper.groupByFirstLetter(
                SignGroupingHelper.sortSignsAlphabetically(
                    loadedFavorites,
                    SortOrder.ASCENDING
                ),
                SortOrder.ASCENDING
            )
        }.onFailure {
            _error.value = it.message ?: "Failed to load favorites"
        }

        _isLoading.value = false
    }
}

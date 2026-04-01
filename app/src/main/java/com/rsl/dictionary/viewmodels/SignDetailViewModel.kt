package com.rsl.dictionary.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class SignDetailViewModel @Inject constructor(
    private val signRepository: SignRepository,
    private val favoritesRepository: FavoritesRepository,
    private val videoRepository: VideoRepository,
    private val analyticsService: AnalyticsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _sign = MutableStateFlow<Sign?>(null)
    val sign: StateFlow<Sign?> = _sign.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _visitedSignIds = MutableStateFlow<Set<String>>(emptySet())
    val visitedSignIds: StateFlow<Set<String>> = _visitedSignIds.asStateFlow()

    init {
        _visitedSignIds.value = savedStateHandle
            .get<String>("visitedSignIds")
            ?.let(::parseVisitedSignIds)
            ?: emptySet()

        viewModelScope.launch {
            favoritesRepository.favoritesFlow.collect { favoriteIds ->
                _isFavorite.value = sign.value?.id?.let { it in favoriteIds } ?: false
            }
        }

        savedStateHandle.get<String>("signId")?.let { signId ->
            loadSign(signId)
        }
    }

    fun loadSign(signId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { signRepository.getSign(signId) }
                .onSuccess { loadedSign ->
                    _sign.value = loadedSign
                    _isFavorite.value = favoritesRepository.isFavorite(loadedSign.id)
                    _visitedSignIds.value = _visitedSignIds.value + loadedSign.id
                    analyticsService.logSignViewed(loadedSign.id, loadedSign.word, loadedSign.categoryId)
                }
                .onFailure { _error.value = ErrorMessageMapper.map(it) }
            _isLoading.value = false
        }
    }

    fun loadSynonymSign(
        signId: String,
        onSuccess: (Sign) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching { signRepository.getSign(signId) }
                .onSuccess { loadedSign ->
                    onSuccess(loadedSign)
                }
                .onFailure { onFailure(ErrorMessageMapper.map(it)) }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentSign = sign.value ?: return@launch
            _error.value = null
            runCatching {
                if (favoritesRepository.isFavorite(currentSign.id)) {
                    favoritesRepository.remove(currentSign.id)
                    videoRepository.clearFavoritesCache(currentSign)
                    analyticsService.logSignUnfavorited(currentSign.id, currentSign.word)
                } else {
                    favoritesRepository.add(currentSign.id)
                    videoRepository.prefetchVideos(currentSign)
                    analyticsService.logSignFavorited(currentSign.id, currentSign.word)
                }
            }.onFailure { _error.value = ErrorMessageMapper.map(it) }
        }
    }

    private fun parseVisitedSignIds(encodedVisitedSignIds: String): Set<String> {
        return runCatching {
            Json.decodeFromString<List<String>>(Uri.decode(encodedVisitedSignIds)).toSet()
        }.getOrDefault(emptySet())
    }
}

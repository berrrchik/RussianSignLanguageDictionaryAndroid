package com.rsl.dictionary.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult
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
import timber.log.Timber

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

    private val _favoriteOfflineStatus = MutableStateFlow<FavoriteOfflineStatus?>(null)
    val favoriteOfflineStatus: StateFlow<FavoriteOfflineStatus?> = _favoriteOfflineStatus.asStateFlow()

    private val _isFavoriteActionInProgress = MutableStateFlow(false)
    val isFavoriteActionInProgress: StateFlow<Boolean> = _isFavoriteActionInProgress.asStateFlow()

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
            favoritesRepository.favoriteEntriesFlow.collect { entries ->
                val currentEntry = sign.value?.id?.let { signId ->
                    entries.firstOrNull { it.signId == signId }
                }
                _isFavorite.value = currentEntry != null
                _favoriteOfflineStatus.value = currentEntry?.status
                sign.value?.id?.let { currentSignId ->
                    Timber.d(
                        "SignDetail: favorites update signId=%s, isFavorite=%s, status=%s, favoritesCount=%d",
                        currentSignId,
                        _isFavorite.value,
                        currentEntry?.status,
                        entries.size
                    )
                }
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
            Timber.d("SignDetail: opening signId=%s", signId)
            runCatching { signRepository.getSign(signId) }
                .onSuccess { loadedSign ->
                    _sign.value = loadedSign
                    val isFavorite = favoritesRepository.isFavorite(loadedSign.id)
                    _isFavorite.value = isFavorite
                    _favoriteOfflineStatus.value = favoritesRepository.getOfflineStatus(loadedSign.id)
                    _visitedSignIds.value = _visitedSignIds.value + loadedSign.id
                    Timber.d(
                        "SignDetail: loaded signId=%s, word=%s, isFavorite=%s, favoriteStatus=%s, videos=%d",
                        loadedSign.id,
                        loadedSign.word,
                        isFavorite,
                        _favoriteOfflineStatus.value,
                        loadedSign.videosArray.size
                    )
                    analyticsService.logSignViewed(loadedSign.id, loadedSign.word, loadedSign.categoryId)
                }
                .onFailure {
                    Timber.e(it, "SignDetail: failed to load signId=%s", signId)
                    _error.value = ErrorMessageMapper.map(it)
                }
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
                    Timber.d(
                        "SignDetail: loaded synonym signId=%s for currentSignId=%s",
                        loadedSign.id,
                        sign.value?.id
                    )
                    onSuccess(loadedSign)
                }
                .onFailure {
                    Timber.e(it, "SignDetail: failed to load synonym signId=%s", signId)
                    onFailure(ErrorMessageMapper.map(it))
                }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentSign = sign.value ?: return@launch
            if (_isFavoriteActionInProgress.value) return@launch
            _error.value = null
            _isFavoriteActionInProgress.value = true
            Timber.d(
                "SignDetail: toggle favorite signId=%s, currentlyFavorite=%s",
                currentSign.id,
                favoritesRepository.isFavorite(currentSign.id)
            )
            runCatching {
                if (favoritesRepository.isFavorite(currentSign.id)) {
                    favoritesRepository.getEntry(currentSign.id)?.let { entry ->
                        videoRepository.removeFavoriteOffline(entry)
                    }
                    favoritesRepository.remove(currentSign.id)
                    Timber.d("SignDetail: signId=%s removed from favorites", currentSign.id)
                    analyticsService.logSignUnfavorited(currentSign.id, currentSign.word)
                } else {
                    favoritesRepository.markFavoritePending(currentSign)
                    when (val preparationResult = videoRepository.prepareFavoriteOffline(currentSign)) {
                        is FavoriteOfflinePreparationResult.Ready -> {
                            favoritesRepository.markFavoriteReady(
                                signId = currentSign.id,
                                downloadedVideos = preparationResult.downloadedVideos
                            )
                            Timber.d(
                                "SignDetail: signId=%s favorite offline media prepared successfully",
                                currentSign.id
                            )
                        }

                        is FavoriteOfflinePreparationResult.Failed -> {
                            favoritesRepository.markFavoriteFailed(
                                signId = currentSign.id,
                                downloadedVideos = preparationResult.downloadedVideos
                            )
                            // Не пишем в _error: знак сохранён в избранное, просто видео
                            // не удалось скачать сейчас. Статус FAILED отобразится меткой
                            // на экране. Повторная попытка произойдёт автоматически при
                            // восстановлении сети через FavoritesViewModel.
                            Timber.w(
                                preparationResult.error,
                                "SignDetail: signId=%s favorite offline preparation failed",
                                currentSign.id
                            )
                        }
                    }
                    analyticsService.logSignFavorited(currentSign.id, currentSign.word)
                }
            }.onFailure {
                Timber.e(it, "SignDetail: failed to toggle favorite for signId=%s", currentSign.id)
                _error.value = ErrorMessageMapper.map(it)
            }.also {
                _isFavoriteActionInProgress.value = false
            }
        }
    }

    private fun parseVisitedSignIds(encodedVisitedSignIds: String): Set<String> {
        return runCatching {
            Json.decodeFromString<List<String>>(Uri.decode(encodedVisitedSignIds)).toSet()
        }.getOrDefault(emptySet())
    }
}
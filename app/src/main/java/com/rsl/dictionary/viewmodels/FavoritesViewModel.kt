package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.utilities.ErrorMessageMapper
import com.rsl.dictionary.utilities.data.SignGroupingHelper
import com.rsl.dictionary.utilities.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val signRepository: SignRepository,
    private val videoRepository: VideoRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    private val _favoriteIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteIds: StateFlow<List<String>> = _favoriteIds.asStateFlow()

    private val _favoriteStatuses = MutableStateFlow<Map<String, FavoriteOfflineStatus>>(emptyMap())
    val favoriteStatuses: StateFlow<Map<String, FavoriteOfflineStatus>> = _favoriteStatuses.asStateFlow()

    private val _favorites = MutableStateFlow<List<Sign>>(emptyList())
    val favorites: StateFlow<List<Sign>> = _favorites.asStateFlow()

    private val _groupedFavorites = MutableStateFlow<Map<String, List<Sign>>>(emptyMap())
    val groupedFavorites: StateFlow<Map<String, List<Sign>>> = _groupedFavorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Храним timestamp последней синхронизации при которой запускали retry,
    // чтобы не запускать его дважды если syncData эмитит одно и то же значение.
    private var lastRetriedSyncTimestamp = 0L

    init {
        viewModelScope.launch {
            favoritesRepository.favoriteEntriesFlow.collect { favoriteEntries ->
                val favoriteIds = favoriteEntries.map { it.signId }
                _favoriteIds.value = favoriteIds
                _favoriteStatuses.value = favoriteEntries.associate { it.signId to it.status }
                Timber.d(
                    "FavoritesViewModel: favoriteEntriesFlow emitted ids=%s statuses=%s",
                    favoriteIds,
                    _favoriteStatuses.value
                )
                loadFavoriteSigns(favoriteEntries)
            }
        }

        viewModelScope.launch {
            signRepository.syncData
                .filterNotNull()
                .collect { syncData ->
                    presentFavoriteSigns(
                        allSigns = syncData.signs,
                        favoriteEntries = favoritesRepository.getEntries()
                    )
                    // Запускаем retry при каждом успешном контакте с сервером —
                    // как при первой синхронизации на старте (сервер доступен),
                    // так и при последующих обновлениях данных.
                    // Сравниваем по lastUpdated чтобы не запускать дважды
                    // если syncData эмитит одинаковое значение.
                    if (syncData.lastUpdated != lastRetriedSyncTimestamp) {
                        lastRetriedSyncTimestamp = syncData.lastUpdated
                        retryFailedFavorites()
                    }
                }
        }

        // При восстановлении сети автоматически повторяем скачивание
        // видео для всех избранных со статусом FAILED.
        viewModelScope.launch {
            networkMonitor.isConnectedFlow
                .drop(1) // пропускаем начальное значение при старте
                .filter { isConnected -> isConnected }
                .collect {
                    retryFailedFavorites()
                }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                favoritesRepository.getEntries().forEach { entry ->
                    videoRepository.removeFavoriteOffline(entry)
                }
                favoritesRepository.clearAll()
            }.onFailure {
                _error.value = ErrorMessageMapper.map(it)
            }
            _isLoading.value = false
        }
    }

    fun retryLoadingFavorites() {
        viewModelScope.launch {
            loadFavoriteSigns(favoritesRepository.getEntries())
        }
    }

    private fun retryFailedFavorites() {
        viewModelScope.launch {
            val failedEntries = favoritesRepository.getEntries()
                .filter { it.status == FavoriteOfflineStatus.FAILED }
            if (failedEntries.isEmpty()) return@launch

            Timber.d(
                "FavoritesViewModel: network restored, retrying %d FAILED favorites",
                failedEntries.size
            )

            val cachedSignsById = favoritesRepository.getCachedSigns().associateBy { it.id }

            failedEntries.forEach { entry ->
                val sign = cachedSignsById[entry.signId] ?: run {
                    Timber.w(
                        "FavoritesViewModel: no cached sign snapshot for signId=%s, skipping retry",
                        entry.signId
                    )
                    return@forEach
                }

                favoritesRepository.markFavoritePending(sign)
                Timber.d(
                    "FavoritesViewModel: retrying offline preparation for signId=%s",
                    entry.signId
                )

                when (val result = videoRepository.prepareFavoriteOffline(sign)) {
                    is FavoriteOfflinePreparationResult.Ready -> {
                        favoritesRepository.markFavoriteReady(
                            signId = sign.id,
                            downloadedVideos = result.downloadedVideos
                        )
                        Timber.d(
                            "FavoritesViewModel: retry succeeded for signId=%s",
                            sign.id
                        )
                    }
                    is FavoriteOfflinePreparationResult.Failed -> {
                        favoritesRepository.markFavoriteFailed(
                            signId = sign.id,
                            downloadedVideos = result.downloadedVideos
                        )
                        Timber.w(
                            result.error,
                            "FavoritesViewModel: retry failed for signId=%s",
                            sign.id
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadFavoriteSigns(favoriteEntries: List<FavoriteEntry>) {
        val favoriteIds = favoriteEntries.map { it.signId }
        if (favoriteIds.isEmpty()) {
            Timber.d("FavoritesViewModel: no favorite ids, showing empty state")
            _favorites.value = emptyList()
            _groupedFavorites.value = emptyMap()
            _error.value = null
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _error.value = null
        Timber.d("FavoritesViewModel: loading favorite signs ids=%s", favoriteIds)

        runCatching {
            presentFavoriteSigns(
                allSigns = signRepository.getAllSigns(),
                favoriteEntries = favoriteEntries
            )
        }.onSuccess { loadedFavorites ->
            loadedFavorites
        }.onFailure { error ->
            val cachedFavoritesById = favoritesRepository.getCachedSigns().associateBy { it.id }
            val cachedFavorites = favoriteIds.mapNotNull { cachedFavoritesById[it] }

            if (cachedFavorites.isNotEmpty()) {
                Timber.w(
                    error,
                    "FavoritesViewModel: falling back to %d cached favorite snapshots for ids=%s",
                    cachedFavorites.size,
                    favoriteIds
                )
                _favorites.value = cachedFavorites
                _groupedFavorites.value = SignGroupingHelper.groupByFirstLetter(
                    SignGroupingHelper.sortSignsAlphabetically(
                        cachedFavorites,
                        SortOrder.ASCENDING
                    ),
                    SortOrder.ASCENDING
                )
            } else {
                Timber.e(
                    error,
                    "FavoritesViewModel: failed to load favorites and no cached snapshots available, ids=%s",
                    favoriteIds
                )
                _favorites.value = emptyList()
                _groupedFavorites.value = emptyMap()
                _error.value = ErrorMessageMapper.map(error)
            }
        }

        _isLoading.value = false
    }

    private suspend fun presentFavoriteSigns(
        allSigns: List<Sign>,
        favoriteEntries: List<FavoriteEntry>
    ): List<Sign> {
        val favoriteIds = favoriteEntries.map { it.signId }
        val allSignsById = allSigns.associateBy { it.id }
        val loadedFavorites = favoriteIds.mapNotNull { allSignsById[it] }
        favoritesRepository.cacheSigns(loadedFavorites)
        Timber.d(
            "FavoritesViewModel: loaded %d favorites from SignRepository",
            loadedFavorites.size
        )
        _favorites.value = loadedFavorites
        _groupedFavorites.value = SignGroupingHelper.groupByFirstLetter(
            SignGroupingHelper.sortSignsAlphabetically(
                loadedFavorites,
                SortOrder.ASCENDING
            ),
            SortOrder.ASCENDING
        )
        _error.value = null
        return loadedFavorites
    }
}
package com.rsl.dictionary.repositories.protocols

import com.rsl.dictionary.models.FavoriteOfflineVideo

sealed interface FavoriteOfflinePreparationResult {
    data class Ready(
        val downloadedVideos: List<FavoriteOfflineVideo>
    ) : FavoriteOfflinePreparationResult

    data class Failed(
        val downloadedVideos: List<FavoriteOfflineVideo>,
        val error: Throwable
    ) : FavoriteOfflinePreparationResult
}

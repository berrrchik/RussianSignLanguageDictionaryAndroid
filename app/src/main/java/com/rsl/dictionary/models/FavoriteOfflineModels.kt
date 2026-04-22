package com.rsl.dictionary.models

import kotlinx.serialization.Serializable

@Serializable
enum class FavoriteOfflineStatus {
    PENDING,
    READY_OFFLINE,
    FAILED
}

@Serializable
data class FavoriteOfflineVideo(
    val videoId: Int,
    val fileName: String
)

@Serializable
data class FavoriteEntry(
    val signId: String,
    val status: FavoriteOfflineStatus,
    val requiredVideoIds: List<Int>,
    val downloadedVideos: List<FavoriteOfflineVideo> = emptyList(),
    val updatedAt: Long
)

package com.rsl.dictionary.repositories.protocols

import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.FavoriteOfflineVideo
import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    suspend fun markFavoritePending(sign: Sign)
    suspend fun markFavoriteReady(signId: String, downloadedVideos: List<FavoriteOfflineVideo>)
    suspend fun markFavoriteFailed(signId: String, downloadedVideos: List<FavoriteOfflineVideo>)
    suspend fun remove(signId: String)
    suspend fun cacheSigns(signs: List<Sign>)
    fun isFavorite(signId: String): Boolean
    fun getAll(): List<String>
    fun getEntry(signId: String): FavoriteEntry?
    fun getEntries(): List<FavoriteEntry>
    fun getOfflineStatus(signId: String): FavoriteOfflineStatus?
    fun getCachedSigns(): List<Sign>
    suspend fun clearAll()
    val favoritesFlow: StateFlow<List<String>>
    val favoriteEntriesFlow: StateFlow<List<FavoriteEntry>>
}

package com.rsl.dictionary.testing.fakes

import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.FavoriteOfflineVideo
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFavoritesRepository(
    initialFavorites: Collection<String> = emptyList()
) : FavoritesRepository {
    private val favorites = linkedMapOf<String, FavoriteEntry>().apply {
        initialFavorites.forEach { signId ->
            put(
                signId,
                FavoriteEntry(
                    signId = signId,
                    status = FavoriteOfflineStatus.READY_OFFLINE,
                    requiredVideoIds = emptyList(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
    private val cachedSigns = linkedMapOf<String, Sign>()
    private val _favoritesFlow = MutableStateFlow(favorites.keys.toList())
    private val _favoriteEntriesFlow = MutableStateFlow(favorites.values.toList())

    override val favoritesFlow: StateFlow<List<String>> = _favoritesFlow.asStateFlow()
    override val favoriteEntriesFlow: StateFlow<List<FavoriteEntry>> = _favoriteEntriesFlow.asStateFlow()

    override suspend fun markFavoritePending(sign: Sign) {
        favorites.remove(sign.id)
        favorites[sign.id] = FavoriteEntry(
            signId = sign.id,
            status = FavoriteOfflineStatus.PENDING,
            requiredVideoIds = sign.videosArray.map { it.id },
            updatedAt = System.currentTimeMillis()
        )
        cacheSigns(listOf(sign))
        publish()
    }

    override suspend fun markFavoriteReady(signId: String, downloadedVideos: List<FavoriteOfflineVideo>) {
        val current = favorites[signId] ?: return
        favorites[signId] = current.copy(
            status = FavoriteOfflineStatus.READY_OFFLINE,
            downloadedVideos = downloadedVideos,
            updatedAt = System.currentTimeMillis()
        )
        publish()
    }

    override suspend fun markFavoriteFailed(signId: String, downloadedVideos: List<FavoriteOfflineVideo>) {
        val current = favorites[signId] ?: return
        favorites[signId] = current.copy(
            status = FavoriteOfflineStatus.FAILED,
            downloadedVideos = downloadedVideos,
            updatedAt = System.currentTimeMillis()
        )
        publish()
    }

    override suspend fun remove(signId: String) {
        favorites.remove(signId)
        cachedSigns.remove(signId)
        publish()
    }

    override suspend fun cacheSigns(signs: List<Sign>) {
        signs.forEach { sign ->
            cachedSigns[sign.id] = sign
        }
    }

    override fun isFavorite(signId: String): Boolean = signId in favorites

    override fun getAll(): List<String> = favorites.keys.toList()

    override fun getEntry(signId: String): FavoriteEntry? = favorites[signId]

    override fun getEntries(): List<FavoriteEntry> = favorites.values.toList()

    override fun getOfflineStatus(signId: String): FavoriteOfflineStatus? = favorites[signId]?.status

    override fun getCachedSigns(): List<Sign> = cachedSigns.values.toList()

    override suspend fun clearAll() {
        favorites.clear()
        cachedSigns.clear()
        publish()
    }

    private fun publish() {
        _favoritesFlow.value = favorites.keys.toList()
        _favoriteEntriesFlow.value = favorites.values.toList()
    }
}

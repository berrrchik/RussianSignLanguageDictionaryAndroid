package com.rsl.dictionary.repositories.impl

import android.content.SharedPreferences
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.FavoriteOfflineVideo
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class FavoritesRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : FavoritesRepository {

    private val _favoritesFlow = MutableStateFlow(getAll())
    override val favoritesFlow: StateFlow<List<String>> = _favoritesFlow.asStateFlow()
    private val _favoriteEntriesFlow = MutableStateFlow(getEntries())
    override val favoriteEntriesFlow: StateFlow<List<FavoriteEntry>> = _favoriteEntriesFlow.asStateFlow()

    override suspend fun markFavoritePending(sign: Sign) {
        val storedEntries = getStoredEntries().associateByTo(LinkedHashMap()) { it.signId }
        storedEntries.remove(sign.id)
        storedEntries[sign.id] = FavoriteEntry(
            signId = sign.id,
            status = FavoriteOfflineStatus.PENDING,
            requiredVideoIds = sign.videosArray.map { it.id },
            updatedAt = System.currentTimeMillis()
        )
        saveEntries(storedEntries.values.toList())
        cacheSigns(listOf(sign))
        Timber.d(
            "FavoritesRepository: signId=%s marked pending, trackedVideos=%d",
            sign.id,
            sign.videosArray.size
        )
    }

    override suspend fun markFavoriteReady(signId: String, downloadedVideos: List<FavoriteOfflineVideo>) {
        updateEntry(signId) { currentEntry ->
            currentEntry.copy(
                status = FavoriteOfflineStatus.READY_OFFLINE,
                downloadedVideos = downloadedVideos,
                updatedAt = System.currentTimeMillis()
            )
        }
        Timber.d(
            "FavoritesRepository: signId=%s marked ready with files=%d",
            signId,
            downloadedVideos.size
        )
    }

    override suspend fun markFavoriteFailed(signId: String, downloadedVideos: List<FavoriteOfflineVideo>) {
        updateEntry(signId) { currentEntry ->
            currentEntry.copy(
                status = FavoriteOfflineStatus.FAILED,
                downloadedVideos = downloadedVideos,
                updatedAt = System.currentTimeMillis()
            )
        }
        Timber.d(
            "FavoritesRepository: signId=%s marked failed, downloadedFiles=%d",
            signId,
            downloadedVideos.size
        )
    }

    override suspend fun remove(signId: String) {
        val updatedEntries = getStoredEntries().filterNot { it.signId == signId }
        saveEntries(updatedEntries)
        removeCachedSign(signId)
        Timber.d("FavoritesRepository: removed signId=%s, totalFavorites=%d", signId, updatedEntries.size)
    }

    override suspend fun cacheSigns(signs: List<Sign>) {
        if (signs.isEmpty()) return

        val cachedSigns = getStoredFavoriteSigns().associateByTo(LinkedHashMap()) { it.id }
        signs.forEach { sign ->
            cachedSigns[sign.id] = sign
        }
        saveCachedSigns(cachedSigns.values.toList())
        Timber.d(
            "FavoritesRepository: cached %d sign snapshots, totalSnapshots=%d",
            signs.size,
            cachedSigns.size
        )
    }

    override fun isFavorite(signId: String): Boolean {
        val isFavorite = getStoredEntries().any { it.signId == signId }
        Timber.d("FavoritesRepository: isFavorite signId=%s -> %s", signId, isFavorite)
        return isFavorite
    }

    override fun getAll(): List<String> = getStoredEntries().map { it.signId }

    override fun getEntry(signId: String): FavoriteEntry? = getStoredEntries().firstOrNull { it.signId == signId }

    override fun getEntries(): List<FavoriteEntry> = getStoredEntries()

    override fun getOfflineStatus(signId: String): FavoriteOfflineStatus? = getEntry(signId)?.status

    override fun getCachedSigns(): List<Sign> {
        val cachedSigns = getStoredFavoriteSigns()
        Timber.d("FavoritesRepository: loaded cached sign snapshots count=%d", cachedSigns.size)
        return cachedSigns
    }

    override suspend fun clearAll() {
        sharedPreferences.edit()
            .remove(FAVORITES_ENTRIES_VALUE_KEY)
            .remove(FAVORITES_SIGNS_VALUE_KEY)
            .apply()
        _favoritesFlow.value = emptyList()
        _favoriteEntriesFlow.value = emptyList()
        Timber.d("FavoritesRepository: cleared all favorites and cached snapshots")
    }

    private fun getStoredFavoriteSigns(): List<Sign> {
        val encodedSigns = sharedPreferences.getString(FAVORITES_SIGNS_VALUE_KEY, null) ?: return emptyList()
        return runCatching {
            ApiJsonDecoder.json.decodeFromString<List<Sign>>(encodedSigns)
        }.getOrDefault(emptyList())
    }

    private fun saveCachedSigns(signs: List<Sign>) {
        val encodedSigns = ApiJsonDecoder.json.encodeToString(signs)
        sharedPreferences.edit()
            .putString(FAVORITES_SIGNS_VALUE_KEY, encodedSigns)
            .apply()
    }

    private fun removeCachedSign(signId: String) {
        val updatedSigns = getStoredFavoriteSigns().filterNot { it.id == signId }
        if (updatedSigns.isEmpty()) {
            sharedPreferences.edit().remove(FAVORITES_SIGNS_VALUE_KEY).apply()
            Timber.d("FavoritesRepository: removed last cached snapshot for signId=%s", signId)
            return
        }
        saveCachedSigns(updatedSigns)
        Timber.d(
            "FavoritesRepository: removed cached snapshot for signId=%s, remainingSnapshots=%d",
            signId,
            updatedSigns.size
        )
    }

    private fun getStoredEntries(): List<FavoriteEntry> {
        val encodedEntries = sharedPreferences.getString(FAVORITES_ENTRIES_VALUE_KEY, null)
        if (encodedEntries == null) {
            return getLegacyEntries()
        }
        return runCatching {
            ApiJsonDecoder.json.decodeFromString<List<FavoriteEntry>>(encodedEntries)
        }.getOrElse {
            getLegacyEntries()
        }
    }

    private fun saveEntries(entries: List<FavoriteEntry>) {
        if (entries.isEmpty()) {
            sharedPreferences.edit()
                .remove(FAVORITES_ENTRIES_VALUE_KEY)
                .remove(LEGACY_FAVORITES_VALUE_KEY)
                .apply()
        } else {
            val encodedEntries = ApiJsonDecoder.json.encodeToString(entries)
            sharedPreferences.edit()
                .putString(FAVORITES_ENTRIES_VALUE_KEY, encodedEntries)
                .remove(LEGACY_FAVORITES_VALUE_KEY)
                .apply()
        }
        _favoriteEntriesFlow.value = entries
        _favoritesFlow.value = entries.map { it.signId }
    }

    private fun updateEntry(
        signId: String,
        transform: (FavoriteEntry) -> FavoriteEntry
    ) {
        val updatedEntries = getStoredEntries().map { entry ->
            if (entry.signId == signId) {
                transform(entry)
            } else {
                entry
            }
        }
        saveEntries(updatedEntries)
    }

    private fun getLegacyEntries(): List<FavoriteEntry> {
        val legacyIds = sharedPreferences.getStringSet(LEGACY_FAVORITES_VALUE_KEY, emptySet()).orEmpty()
        if (legacyIds.isEmpty()) return emptyList()

        val cachedSignsById = getStoredFavoriteSigns().associateBy { it.id }
        return legacyIds.map { signId ->
            FavoriteEntry(
                signId = signId,
                status = FavoriteOfflineStatus.FAILED,
                requiredVideoIds = cachedSignsById[signId]?.videosArray?.map { it.id }.orEmpty(),
                updatedAt = 0L
            )
        }
    }

    private companion object {
        const val FAVORITES_ENTRIES_VALUE_KEY = "favorite_offline_entries"
        const val FAVORITES_SIGNS_VALUE_KEY = "favorite_sign_snapshots"
        const val LEGACY_FAVORITES_VALUE_KEY = "favorite_sign_ids"
    }
}

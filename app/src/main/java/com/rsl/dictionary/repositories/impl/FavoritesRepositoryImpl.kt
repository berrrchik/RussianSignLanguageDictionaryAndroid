package com.rsl.dictionary.repositories.impl

import android.content.SharedPreferences
import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : FavoritesRepository {

    private val _favoritesFlow = MutableStateFlow(getAll())
    override val favoritesFlow: StateFlow<List<String>> = _favoritesFlow.asStateFlow()

    override suspend fun add(signId: String) {
        val updatedFavorites = LinkedHashSet(getAll()).apply {
            remove(signId)
            add(signId)
        }
        saveFavorites(updatedFavorites)
    }

    override suspend fun remove(signId: String) {
        val updatedFavorites = LinkedHashSet(getAll()).apply {
            remove(signId)
        }
        saveFavorites(updatedFavorites)
    }

    override fun isFavorite(signId: String): Boolean = getStoredFavorites().contains(signId)

    override fun getAll(): List<String> = getStoredFavorites().toList()

    override suspend fun clearAll() {
        sharedPreferences.edit().remove(FAVORITES_VALUE_KEY).apply()
        _favoritesFlow.value = emptyList()
    }

    private fun getStoredFavorites(): LinkedHashSet<String> {
        val storedSet = sharedPreferences.getStringSet(FAVORITES_VALUE_KEY, emptySet()).orEmpty()
        return LinkedHashSet(storedSet)
    }

    private fun saveFavorites(favorites: LinkedHashSet<String>) {
        sharedPreferences.edit()
            .putStringSet(FAVORITES_VALUE_KEY, LinkedHashSet(favorites))
            .apply()
        _favoritesFlow.value = favorites.toList()
    }

    private companion object {
        const val FAVORITES_VALUE_KEY = "favorite_sign_ids"
    }
}

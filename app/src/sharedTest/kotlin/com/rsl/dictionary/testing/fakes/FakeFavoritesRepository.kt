package com.rsl.dictionary.testing.fakes

import com.rsl.dictionary.repositories.protocols.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFavoritesRepository(
    initialFavorites: Collection<String> = emptyList()
) : FavoritesRepository {
    private val favorites = LinkedHashSet(initialFavorites)
    private val _favoritesFlow = MutableStateFlow(favorites.toList())

    override val favoritesFlow: StateFlow<List<String>> = _favoritesFlow.asStateFlow()

    override suspend fun add(signId: String) {
        favorites.add(signId)
        publish()
    }

    override suspend fun remove(signId: String) {
        favorites.remove(signId)
        publish()
    }

    override fun isFavorite(signId: String): Boolean = signId in favorites

    override fun getAll(): List<String> = favorites.toList()

    override suspend fun clearAll() {
        favorites.clear()
        publish()
    }

    private fun publish() {
        _favoritesFlow.value = favorites.toList()
    }
}

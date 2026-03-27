package com.rsl.dictionary.repositories.protocols

import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    suspend fun add(signId: String)
    suspend fun remove(signId: String)
    fun isFavorite(signId: String): Boolean
    fun getAll(): List<String>
    suspend fun clearAll()
    val favoritesFlow: StateFlow<List<String>>
}

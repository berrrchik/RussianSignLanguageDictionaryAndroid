package com.rsl.dictionary.repositories.protocols

import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SyncData

interface SignRepository {
    suspend fun loadDataWithSync(): SyncData
    suspend fun getSign(byId: String): Sign
    suspend fun getAllSigns(): List<Sign>
    suspend fun getSignsByCategory(categoryId: String): List<Sign>
}

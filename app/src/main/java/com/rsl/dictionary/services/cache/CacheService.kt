package com.rsl.dictionary.services.cache

import android.content.Context
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import com.rsl.dictionary.utilities.data.DecodingErrorLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class CacheService @Inject constructor(
    @ApplicationContext context: Context
) {
    private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun save(data: SyncData) = withContext(Dispatchers.IO) {
        val encodedData = ApiJsonDecoder.json.encodeToString(data)
        cacheFile.writeText(encodedData)
    }

    suspend fun load(): SyncData? = withContext(Dispatchers.IO) {
        try {
            if (!cacheFile.exists()) return@withContext null
            val encodedData = cacheFile.readText()
            ApiJsonDecoder.json.decodeFromString<SyncData>(encodedData)
        } catch (error: Throwable) {
            DecodingErrorLogger.log(error, "CacheService.load")
            null
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    private companion object {
        const val CACHE_FILE_NAME = "cached_signs_data.json"
    }
}

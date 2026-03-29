package com.rsl.dictionary.utilities.cache

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileCacheLRU {
    const val SHORT_TERM_MAX_SIZE_BYTES = 150L * 1024L * 1024L
    const val FAVORITES_MAX_SIZE_BYTES = 500L * 1024L * 1024L
    const val TARGET_PERCENT = 80

    suspend fun enforceSizeLimit(
        directory: File,
        maxSizeBytes: Long,
        targetPercent: Int
    ) = withContext(Dispatchers.IO) {
        val files = directory.listFiles()?.filter { it.isFile } ?: return@withContext
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= maxSizeBytes) return@withContext

        val targetSize = maxSizeBytes * targetPercent / 100
        val sortedFiles = files.sortedBy { it.lastModified() }

        for (file in sortedFiles) {
            if (totalSize <= targetSize) break
            val fileSize = file.length()
            if (file.delete()) {
                totalSize -= fileSize
            }
        }
    }
}

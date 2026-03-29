package com.rsl.dictionary.services.cache

import android.content.Context
import java.io.File
import java.security.MessageDigest
import timber.log.Timber

object VideoCacheDirectoryManager {
    fun videoId(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun shortTermDir(context: Context): File {
        return File(context.cacheDir, SHORT_TERM_DIRECTORY_NAME).apply { mkdirs() }
    }

    fun favoritesDir(context: Context): File {
        return File(context.cacheDir, FAVORITES_DIRECTORY_NAME).apply { mkdirs() }
    }

    fun deleteFile(file: File) {
        runCatching {
            if (file.exists() && !file.delete()) {
                error("Failed to delete file: ${file.absolutePath}")
            }
        }.onFailure { error ->
            Timber.e(error, "Failed to delete cached video file")
        }
    }

    private const val SHORT_TERM_DIRECTORY_NAME = "video_short_term_cache"
    private const val FAVORITES_DIRECTORY_NAME = "favorites_videos"
}

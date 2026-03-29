package com.rsl.dictionary.services.cache

import android.content.Context
import android.net.Uri
import android.util.LruCache
import com.rsl.dictionary.config.ApiConfig
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.utilities.cache.FileCacheLRU
import com.rsl.dictionary.utilities.cache.VideoDownloadCoordinator
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class VideoCacheService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val videoDownloadCoordinator: VideoDownloadCoordinator
) {
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inMemoryCache = LruCache<Int, Uri>(IN_MEMORY_CACHE_SIZE)

    fun getFromMemory(video: SignVideo): Uri? = inMemoryCache.get(video.id)

    suspend fun getOrDownload(video: SignVideo, targetDir: File, context: Context): Uri {
        val cachedInMemory = inMemoryCache.get(video.id)
        if (cachedInMemory != null) {
            touchFile(cachedInMemory)
            return cachedInMemory
        }

        val targetFile = videoFile(video, targetDir)
        if (targetFile.exists()) {
            return Uri.fromFile(targetFile).also { uri ->
                targetFile.setLastModified(System.currentTimeMillis())
                inMemoryCache.put(video.id, uri)
            }
        }

        targetDir.mkdirs()
        val downloadedUri = videoDownloadCoordinator.download(video.id, downloadScope) {
            downloadVideo(video, targetFile, context)
        }
        inMemoryCache.put(video.id, downloadedUri)

        FileCacheLRU.enforceSizeLimit(
            directory = targetDir,
            maxSizeBytes = maxSizeBytes(targetDir),
            targetPercent = FileCacheLRU.TARGET_PERCENT
        )

        return downloadedUri
    }

    suspend fun isCached(video: SignVideo, directory: File): Boolean = withContext(Dispatchers.IO) {
        videoFile(video, directory).exists()
    }

    suspend fun clearDirectory(directory: File) = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { file ->
            VideoCacheDirectoryManager.deleteFile(file)
        }
    }

    private suspend fun downloadVideo(video: SignVideo, targetFile: File, context: Context): Uri {
        val resolvedUrl = ApiConfig.videoUrl(video.url) ?: throw VideoRepositoryError.UrlNotFound
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(resolvedUrl)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Video download failed with HTTP ${response.code}")
                }

                val body = response.body ?: throw IOException("Empty response body")
                targetFile.outputStream().use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                targetFile.setLastModified(System.currentTimeMillis())
                Uri.fromFile(targetFile)
            }
        }
    }

    private fun touchFile(uri: Uri) {
        uri.path?.let { path ->
            File(path).takeIf { it.exists() }?.setLastModified(System.currentTimeMillis())
        }
    }

    private fun videoFile(video: SignVideo, directory: File): File {
        return File(directory, "video_${video.id}.mp4")
    }

    private fun maxSizeBytes(directory: File): Long {
        return if (directory.name == FAVORITES_DIRECTORY_NAME) {
            FileCacheLRU.FAVORITES_MAX_SIZE_BYTES
        } else {
            FileCacheLRU.SHORT_TERM_MAX_SIZE_BYTES
        }
    }

    private companion object {
        const val IN_MEMORY_CACHE_SIZE = 20
        const val FAVORITES_DIRECTORY_NAME = "favorites_videos"
    }
}

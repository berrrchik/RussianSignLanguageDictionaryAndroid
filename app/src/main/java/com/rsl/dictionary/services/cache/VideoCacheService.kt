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
import java.util.concurrent.TimeUnit
import timber.log.Timber

class VideoCacheService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val videoDownloadCoordinator: VideoDownloadCoordinator
) {
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inMemoryCache = LruCache<Int, Uri>(IN_MEMORY_CACHE_SIZE)
    private val videoHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(VIDEO_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(VIDEO_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(VIDEO_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    init {
        Timber.d(
            "VideoCacheService: initialized (timeouts: connect=%ss read=%ss call=%ss)",
            VIDEO_CONNECT_TIMEOUT_SECONDS,
            VIDEO_READ_TIMEOUT_SECONDS,
            VIDEO_CALL_TIMEOUT_SECONDS
        )
    }

    fun getFromMemory(video: SignVideo): Uri? = inMemoryCache.get(video.id)

    fun clearInMemoryCache() {
        inMemoryCache.evictAll()
    }

    suspend fun getOrDownload(video: SignVideo, targetDir: File, context: Context): Uri {
        val cachedInMemory = inMemoryCache.get(video.id)
        if (cachedInMemory != null) {
            Timber.d("VideoCacheService: memory cache hit for videoId=%d", video.id)
            touchFile(cachedInMemory)
            return cachedInMemory
        }

        val targetFile = videoFile(video, targetDir)
        if (targetFile.exists()) {
            Timber.d(
                "VideoCacheService: disk cache hit for videoId=%d path=%s",
                video.id,
                targetFile.absolutePath
            )
            return Uri.fromFile(targetFile).also { uri ->
                targetFile.setLastModified(System.currentTimeMillis())
                inMemoryCache.put(video.id, uri)
            }
        }

        targetDir.mkdirs()
        logDirectoryState("before download", targetDir)
        val downloadedUri = videoDownloadCoordinator.download(video.id, downloadScope) {
            downloadVideo(video, targetFile, context)
        }
        inMemoryCache.put(video.id, downloadedUri)
        Timber.d(
            "VideoCacheService: cached downloaded videoId=%d in memory and disk path=%s",
            video.id,
            targetFile.absolutePath
        )

        FileCacheLRU.enforceSizeLimit(
            directory = targetDir,
            maxSizeBytes = maxSizeBytes(targetDir),
            targetPercent = FileCacheLRU.TARGET_PERCENT
        )

        return downloadedUri
    }

    suspend fun isCached(video: SignVideo, directory: File): Boolean = withContext(Dispatchers.IO) {
        val targetFile = videoFile(video, directory)
        val exists = targetFile.exists()
        Timber.d(
            "VideoCacheService: cache lookup videoId=%d dir=%s exists=%s",
            video.id,
            directory.absolutePath,
            exists
        )
        exists
    }

    suspend fun clearDirectory(directory: File) = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { file ->
            VideoCacheDirectoryManager.deleteFile(file)
        }
        Timber.d("VideoCacheService: cleared directory=%s", directory.absolutePath)
    }

    private suspend fun downloadVideo(video: SignVideo, targetFile: File, context: Context): Uri {
        val resolvedUrl = ApiConfig.videoUrl(video.url) ?: throw VideoRepositoryError.UrlNotFound
        Timber.d(
            "VideoCacheService: downloading videoId=%d from %s to %s",
            video.id,
            resolvedUrl,
            targetFile.absolutePath
        )
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(resolvedUrl)
                .get()
                .build()

            videoHttpClient.newCall(request).execute().use { response ->
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
                Timber.d(
                    "VideoCacheService: saved videoId=%d sizeKb=%d path=%s",
                    video.id,
                    targetFile.length() / 1024,
                    targetFile.absolutePath
                )
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

    private fun logDirectoryState(stage: String, directory: File) {
        val fileNames = directory.listFiles()?.map { it.name }.orEmpty()
        Timber.d(
            "VideoCacheService: %s dir=%s fileCount=%d files=%s",
            stage,
            directory.absolutePath,
            fileNames.size,
            fileNames
        )
    }

    private companion object {
        const val IN_MEMORY_CACHE_SIZE = 20
        const val FAVORITES_DIRECTORY_NAME = "favorites_videos"
        const val VIDEO_CONNECT_TIMEOUT_SECONDS = 3L
        const val VIDEO_READ_TIMEOUT_SECONDS = 6L
        const val VIDEO_CALL_TIMEOUT_SECONDS = 8L
    }
}
